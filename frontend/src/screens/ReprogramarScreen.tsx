import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./ProfesionalDetailScreen.module.css";

import { formatFecha } from "../utils/formatters";
import { RangoDTO, generarSlots, diaSemanaDeISO, generarCalendario, MesCalendario } from "../utils/calendario";

type AgendaDTO = {
  id: number;
  profesionalId: number;
  bloqueMinutos: number;
  mesesAnticipacion: number;
  rangos: RangoDTO[];
};

type TurnoDetalleDTO = {
  id: number;
  agendaId: number;
  clienteId: number;
  clienteNombre: string;
  clienteApellido: string;
  profesionalId: number;
  profesionalNombre: string;
  profesionalApellido: string;
  servicioId: number | null;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};

type TurnoPublicoDTO = { id: number; agendaId: number; fecha: string; bloqueHorario: string; estado: string };
type AgendaPublicaDetailDTO = {
  agenda: AgendaDTO;
  turnos: TurnoPublicoDTO[];
  servicios: { id: number; nombre: string; precio: number }[];
};

type Props = { turnoId: number };

export const ReprogramarScreen = ({ turnoId }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();

  const [turno, setTurno] = useState<TurnoDetalleDTO | null>(null);
  const [agendaDetail, setAgendaDetail] = useState<AgendaPublicaDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [fechaSeleccionada, setFechaSeleccionada] = useState("");
  const [slotSeleccionado, setSlotSeleccionado] = useState("");
  const [reprogramando, setReprogramando] = useState(false);
  const [reprogramaMsg, setReprogramaMsg] = useState("");
  const [exito, setExito] = useState(false);
  const [mesIndex, setMesIndex] = useState(0);

  const calendario: MesCalendario[] = (() => {
    if (!agendaDetail) return [];
    return generarCalendario(agendaDetail.agenda.rangos, agendaDetail.agenda.mesesAnticipacion);
  })();

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    (async () => {
      try {
        const resTurno = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`);
        if (!active) return;
        if (!resTurno.ok) {
          setError("No se pudo cargar el turno a reprogramar.");
          return;
        }
        const turnoData: TurnoDetalleDTO = await resTurno.json();
        if (turnoData.estado !== "REPROGRAMAR") {
          setError("Este turno no está disponible para reprogramar.");
          return;
        }
        setTurno(turnoData);

        const resAgenda = await authedFetch(`${BASE_API_URL}/agenda/profesional/${turnoData.profesionalId}`);
        if (!active) return;
        if (!resAgenda.ok) {
          setError("No se pudo cargar la agenda del profesional.");
          return;
        }
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
  }, [turnoId]);

  const slotsDisponibles: string[] = (() => {
    if (!agendaDetail || !fechaSeleccionada) return [];
    const dia = diaSemanaDeISO(fechaSeleccionada);
    const todos = generarSlots(agendaDetail.agenda.rangos, agendaDetail.agenda.bloqueMinutos, dia);
    const ocupados = new Set(
      agendaDetail.turnos
        .filter((t) => t.fecha === fechaSeleccionada && t.estado !== "REPROGRAMAR" && t.estado !== "CANCELADO")
        .map((t) => t.bloqueHorario),
    );
    return todos.filter((s) => !ocupados.has(s));
  })();

  const confirmar = async () => {
    if (!fechaSeleccionada || !slotSeleccionado) return;
    setReprogramando(true);
    setReprogramaMsg("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}/reprogramar`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fecha: fechaSeleccionada, bloqueHorario: slotSeleccionado }),
      });
      if (res.status === 409) {
        setReprogramaMsg("Ese horario ya fue tomado, elegí otro.");
        return;
      }
      if (!res.ok) {
        const json = await res.json().catch(() => null);
        const detail = json?.message ?? json?.error ?? `Error ${res.status}`;
        setReprogramaMsg(detail);
        return;
      }
      setExito(true);
    } catch {
      setReprogramaMsg("Error de conexión.");
    } finally {
      setReprogramando(false);
    }
  };

  if (exito) {
    return (
      <CommonLayout>
        <div className={styles.page}>
          <div className={styles.card}>
            <h2 className={styles.sectionTitle}>¡Turno reprogramado!</h2>
            <p className={styles.hint}>
              Tu turno con {turno?.profesionalNombre} {turno?.profesionalApellido} quedó confirmado para el{" "}
              {fechaSeleccionada && formatFecha(fechaSeleccionada)} a las {slotSeleccionado}hs. No fue necesario volver
              a pagar la seña.
            </p>
            <button className={styles.btnReservar} onClick={() => navigate("/profesionales")}>
              Volver al inicio
            </button>
          </div>
        </div>
      </CommonLayout>
    );
  }

  return (
    <CommonLayout>
      <div className={styles.page}>
        <button className={styles.btnVolver} onClick={() => navigate("/profesionales")}>
          ← Volver
        </button>

        {loading && <p className={styles.hint}>Cargando...</p>}
        {error && <p className={styles.errorMsg}>{error}</p>}

        {!loading && !error && turno && agendaDetail && (
          <div className={styles.contenido}>
            <section className={styles.card}>
              <h2 className={styles.sectionTitle}>Reprogramar turno</h2>
              <p className={styles.hint}>
                Tu turno con{" "}
                <strong>
                  {turno.profesionalNombre} {turno.profesionalApellido}
                </strong>{" "}
                del <strong>{formatFecha(turno.fecha)}</strong> a las <strong>{turno.bloqueHorario}hs</strong> fue
                cancelado por el profesional. Como ya tenés la seña registrada, no es necesario volver a pagar. Elegí un
                nuevo día y horario:
              </p>
            </section>

            <section className={styles.card}>
              <h2 className={styles.sectionTitle}>Elegí un día</h2>
              {calendario.length === 0 ? (
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
                              setReprogramaMsg("");
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
                          setReprogramaMsg("");
                        }}
                      >
                        {slot}hs
                      </button>
                    ))}
                  </div>
                )}

                {slotSeleccionado && (
                  <div className={styles.confirmBox}>
                    <p className={styles.confirmText}>
                      Nuevo turno: <strong>{formatFecha(fechaSeleccionada)}</strong> a las{" "}
                      <strong>{slotSeleccionado}hs</strong>
                    </p>
                    {reprogramaMsg && <p className={styles.errorMsg}>{reprogramaMsg}</p>}
                    <button className={styles.btnReservar} onClick={confirmar} disabled={reprogramando}>
                      {reprogramando ? "Reprogramando..." : "Confirmar reprogramación"}
                    </button>
                  </div>
                )}
              </section>
            )}
          </div>
        )}
      </div>
    </CommonLayout>
  );
};
