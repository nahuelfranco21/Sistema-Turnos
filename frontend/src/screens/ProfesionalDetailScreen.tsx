import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch, useToken } from "@/services/TokenContext";

import styles from "./ProfesionalDetailScreen.module.css";

import { formatFecha } from "../utils/formatters";
import { Avatar } from "../components/Avatar/Avatar";
import { RangoDTO, generarSlots, diaSemanaDeISO, generarCalendario, MesCalendario } from "../utils/calendario";

type ProfesionalDTO = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  profesion: string | null;
  sector: string | null;
  ubicacion: string | null;
  descripcion: string | null;
  fotoPerfil: string | null;
};
type AgendaDTO = {
  id: number;
  profesionalId: number;
  bloqueMinutos: number;
  mesesAnticipacion: number;
  rangos: RangoDTO[];
};
type TurnoPublicoDTO = { id: number; agendaId: number; fecha: string; bloqueHorario: string; estado: string };
type ServicioDTO = { id: number; nombre: string; precio: number };
type AgendaPublicaDetailDTO = { agenda: AgendaDTO; turnos: TurnoPublicoDTO[]; servicios: ServicioDTO[] };

type Props = { profesionalId: number };

export const ProfesionalDetailScreen = ({ profesionalId }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const [tokenState] = useToken();
  const [, navigate] = useLocation();

  const isProfesional = tokenState.state === "LOGGED_IN" && tokenState.tokens.roles.includes("PROFESIONAL");

  const [profesional, setProfesional] = useState<ProfesionalDTO | null>(null);
  const [agendaDetail, setAgendaDetail] = useState<AgendaPublicaDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [fechaSeleccionada, setFechaSeleccionada] = useState("");
  const [slotSeleccionado, setSlotSeleccionado] = useState("");
  const [servicioSeleccionado, setServicioSeleccionado] = useState<ServicioDTO | null>(null);
  const [reservando, setReservando] = useState(false);
  const [reservaMsg, setReservaMsg] = useState("");
  const [mesIndex, setMesIndex] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    (async () => {
      try {
        const [resProfesional, resAgenda] = await Promise.all([
          authedFetch(`${BASE_API_URL}/users/${profesionalId}`),
          authedFetch(`${BASE_API_URL}/agenda/profesional/${profesionalId}`),
        ]);
        if (!active) return;
        if (!resProfesional.ok || !resAgenda.ok) {
          setError("No se pudo cargar la información del profesional.");
          return;
        }
        setProfesional(await resProfesional.json());
        setAgendaDetail(await resAgenda.json());
      } catch {
        if (active) setError("Error de conexión.");
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [profesionalId]);

  const calendario: MesCalendario[] = (() => {
    if (!agendaDetail) return [];
    return generarCalendario(agendaDetail.agenda.rangos, agendaDetail.agenda.mesesAnticipacion);
  })();

  const slotsDisponibles: string[] = (() => {
    if (!agendaDetail || !fechaSeleccionada) return [];
    const dia = diaSemanaDeISO(fechaSeleccionada);
    const todos = generarSlots(agendaDetail.agenda.rangos, agendaDetail.agenda.bloqueMinutos, dia);
    const ocupados = new Set(
      agendaDetail.turnos.filter((t) => t.fecha === fechaSeleccionada).map((t) => t.bloqueHorario),
    );
    return todos.filter((s) => !ocupados.has(s));
  })();

  const hayServicios = (agendaDetail?.servicios ?? []).length > 0;
  const reservaLista = slotSeleccionado && (!hayServicios || servicioSeleccionado !== null);

  const reservar = async () => {
    if (!agendaDetail || !fechaSeleccionada || !slotSeleccionado) return;
    setReservando(true);
    setReservaMsg("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          agendaId: agendaDetail.agenda.id,
          fecha: fechaSeleccionada,
          bloqueHorario: slotSeleccionado,
          servicioId: servicioSeleccionado?.id ?? null,
        }),
      });
      if (res.status === 409) {
        setReservaMsg("Ese horario ya fue tomado, elegí otro.");
        return;
      }
      if (!res.ok) {
        const json = await res.json().catch(() => null);
        const detail = json?.message ?? json?.error ?? `Error ${res.status}`;
        setReservaMsg(detail);
        return;
      }
      const turnoData = await res.json();
      navigate(`/pago/${turnoData.id}`);
    } catch {
      setReservaMsg("Error de conexión.");
    } finally {
      setReservando(false);
    }
  };

  return (
    <CommonLayout>
      <div className={styles.page}>
        <button className={styles.btnVolver} onClick={() => navigate("/profesionales")}>
          ← Volver
        </button>

        {loading && <p className={styles.hint}>Cargando...</p>}
        {error && <p className={styles.errorMsg}>{error}</p>}

        {!loading && !error && profesional && agendaDetail && (
          <div className={styles.contenido}>
            {/* ── header profesional ── */}
            <div className={styles.header}>
              <Avatar src={profesional.fotoPerfil} nombre={profesional.nombre} apellido={profesional.apellido} size={56} />
              <div>
                <h1 className={styles.nombre}>
                  {profesional.nombre} {profesional.apellido}
                </h1>
                <p className={styles.email}>{profesional.email}</p>
                <p className={styles.email}>
                  {["Sector", "Profesión", "Ubicación"]
                    .map((label, i) => {
                      const val = [profesional.sector, profesional.profesion, profesional.ubicacion][i];
                      return val ? `${label}: ${val}` : null;
                    })
                    .filter(Boolean)
                    .join(" - ")}
                </p>
                {profesional.descripcion && <p className={styles.descripcion}>{profesional.descripcion}</p>}
              </div>
            </div>

            {isProfesional ? (
              <section className={styles.card}>
                <p className={styles.hint}>
                  Solo los clientes pueden reservar turnos. Iniciá sesión con una cuenta de cliente.
                </p>
              </section>
            ) : (
              <>
                {/* Paso 1: elegir servicio */}
                {hayServicios && (
                  <section className={styles.card}>
                    <h2 className={styles.sectionTitle}>Elegí un servicio</h2>
                    <div className={styles.slotsGrid}>
                      {agendaDetail.servicios.map((s) => (
                        <button
                          key={s.id}
                          className={`${styles.slotBtn} ${servicioSeleccionado?.id === s.id ? styles.slotSeleccionado : ""}`}
                          onClick={() => {
                            setServicioSeleccionado(s);
                            setFechaSeleccionada("");
                            setSlotSeleccionado("");
                            setReservaMsg("");
                            setMesIndex(0);
                          }}
                        >
                          {s.nombre} — ${s.precio}
                        </button>
                      ))}
                    </div>
                  </section>
                )}

                {/* Paso 2: selector de fecha (solo si hay servicio seleccionado o no hay servicios) */}
                {(!hayServicios || servicioSeleccionado) && (
                  <section className={styles.card}>
                    <h2 className={styles.sectionTitle}>Elegí un día</h2>
                    {calendario.length === 0 ? (
                      <p className={styles.hint}>No hay días disponibles para agendar.</p>
                    ) : calendario.length === 0 ? (
                      <p className={styles.hint}>No hay días disponibles para agendar.</p>
                    ) : (
                      <div className={styles.calendario}>
                        <div className={styles.mesNav}>
                          <button
                            className={styles.mesNavBtn}
                            disabled={mesIndex === 0}
                            onClick={() => setMesIndex((i) => i - 1)}
                          >
                            ‹
                          </button>
                          <h3 className={styles.mesLabel}>{calendario[mesIndex]?.label ?? ""}</h3>
                          <button
                            className={styles.mesNavBtn}
                            disabled={mesIndex >= calendario.length - 1}
                            onClick={() => setMesIndex((i) => i + 1)}
                          >
                            ›
                          </button>
                        </div>
                        <div className={styles.diasHeader}>
                          <span>Lun</span>
                          <span>Mar</span>
                          <span>Mié</span>
                          <span>Jue</span>
                          <span>Vie</span>
                          <span>Sáb</span>
                          <span>Dom</span>
                        </div>
                        {calendario[mesIndex]?.semanas.map((semana, i) => (
                          <div key={i} className={styles.semana}>
                            {semana.map((celda, j) =>
                              celda ? (
                                <button
                                  key={`${celda.iso}-${j}`}
                                  className={`${styles.diaBtn} ${fechaSeleccionada === celda.iso ? styles.diaSeleccionado : ""}`}
                                  onClick={() => {
                                    setFechaSeleccionada(celda.iso);
                                    setSlotSeleccionado("");
                                    setReservaMsg("");
                                  }}
                                >
                                  {celda.numero}
                                </button>
                              ) : (
                                <div key={`e-${j}`} className={styles.diaVacio} />
                              ),
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </section>
                )}

                {/* Paso 3: slots disponibles */}
                {fechaSeleccionada && (
                  <section className={styles.card}>
                    <h2 className={styles.sectionTitle}>Horarios disponibles</h2>
                    {slotsDisponibles.length === 0 ? (
                      <p className={styles.hint}>No hay turnos disponibles para ese día.</p>
                    ) : (
                      <div className={styles.slotsGrid}>
                        {slotsDisponibles.map((slot) => (
                          <button
                            key={slot}
                            className={`${styles.slotBtn} ${slotSeleccionado === slot ? styles.slotSeleccionado : ""}`}
                            onClick={() => {
                              setSlotSeleccionado(slot);
                              setReservaMsg("");
                            }}
                          >
                            {slot}hs
                          </button>
                        ))}
                      </div>
                    )}

                    {/* Paso 4: confirmar */}
                    {reservaLista && (
                      <div className={styles.confirmBox}>
                        <p className={styles.confirmText}>
                          Reservar turno el <strong>{formatFecha(fechaSeleccionada)}</strong> a las{" "}
                          <strong>{slotSeleccionado}hs</strong>
                        </p>
                        {servicioSeleccionado && (
                          <p className={styles.confirmText}>
                            Servicio: <strong>{servicioSeleccionado.nombre}</strong> — ${servicioSeleccionado.precio}
                          </p>
                        )}
                        {reservaMsg && <p className={styles.errorMsg}>{reservaMsg}</p>}
                        <button className={styles.btnReservar} onClick={reservar} disabled={reservando}>
                          {reservando ? "Reservando..." : "Ir a pagar"}
                        </button>
                      </div>
                    )}
                  </section>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </CommonLayout>
  );
};
