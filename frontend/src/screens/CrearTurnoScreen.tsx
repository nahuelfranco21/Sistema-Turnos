import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import searchStyles from "./PedirTurnoScreen.module.css";
import styles from "./ProfesionalDetailScreen.module.css";

import { formatFecha } from "../utils/formatters";
import { useDebounce } from "../hooks/useDebounce";
import { Avatar } from "../components/Avatar/Avatar";
import { RangoDTO, generarSlots, diaSemanaDeISO, generarCalendario, MesCalendario } from "../utils/calendario";
type AgendaDTO = {
  id: number;
  profesionalId: number;
  bloqueMinutos: number;
  mesesAnticipacion: number;
  rangos: RangoDTO[];
};
type TurnoPublicoDTO = { id: number; agendaId: number; fecha: string; bloqueHorario: string; estado: string };
type AgendaDetailDTO = { agenda: AgendaDTO; turnos: TurnoPublicoDTO[] };
type UsuarioDTO = { id: number; nombre: string; apellido: string; email: string; fotoPerfil: string | null };

export const CrearTurnoScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();

  const [agendaDetail, setAgendaDetail] = useState<AgendaDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [busqueda, setBusqueda] = useState("");
  const [clientes, setClientes] = useState<UsuarioDTO[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [clienteSeleccionado, setClienteSeleccionado] = useState<UsuarioDTO | null>(null);
  const [sinUsuario, setSinUsuario] = useState(false);
  const [sinUsuarioStep, setSinUsuarioStep] = useState<"name" | "calendar">("name");
  const [nombreSinUsuario, setNombreSinUsuario] = useState("");

  const [fechaSeleccionada, setFechaSeleccionada] = useState("");
  const [slotSeleccionado, setSlotSeleccionado] = useState("");
  const [creando, setCreando] = useState(false);
  const [msg, setMsg] = useState("");
  const [mesIndex, setMesIndex] = useState(0);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await authedFetch(`${BASE_API_URL}/agenda/mi-agenda`);
        if (!active) return;
        if (!res.ok) {
          setError("No se pudo cargar la agenda.");
          return;
        }
        setAgendaDetail(await res.json());
      } catch {
        if (active) setError("Error de conexión.");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const debouncedBusqueda = useDebounce(busqueda, 300);

  useEffect(() => {
    if (debouncedBusqueda.trim().length < 2) {
      setClientes([]);
      return;
    }
    let active = true;
    (async () => {
      setBuscando(true);
      try {
        const res = await authedFetch(`${BASE_API_URL}/users?role=CLIENTE&q=${encodeURIComponent(debouncedBusqueda)}`);
        if (active && res.ok) setClientes(await res.json());
      } finally {
        if (active) setBuscando(false);
      }
    })();
    return () => { active = false; };
  }, [debouncedBusqueda]);

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

  const nombreCliente = sinUsuario
    ? nombreSinUsuario.trim()
    : clienteSeleccionado
      ? `${clienteSeleccionado.nombre} ${clienteSeleccionado.apellido}`
      : "";

  const crearTurno = async () => {
    if (!agendaDetail || !fechaSeleccionada || !slotSeleccionado) return;
    if (!sinUsuario && !clienteSeleccionado) return;
    if (sinUsuario && !nombreSinUsuario.trim()) {
      setMsg("Ingresá el nombre del cliente");
      return;
    }
    setCreando(true);
    setMsg("");
    try {
      const body: Record<string, unknown> = {
        agendaId: agendaDetail.agenda.id,
        fecha: fechaSeleccionada,
        bloqueHorario: slotSeleccionado,
      };
      if (sinUsuario) {
        body.clienteId = null;
        body.nombreCliente = nombreSinUsuario.trim();
      } else {
        body.clienteId = clienteSeleccionado!.id;
      }
      const res = await authedFetch(`${BASE_API_URL}/turno`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (res.status === 409) {
        setMsg("Ese bloque ya está ocupado");
        return;
      }
      if (!res.ok) throw new Error(`Error ${res.status}`);
      navigate("/agenda");
    } catch (e: unknown) {
      setMsg(e instanceof Error ? e.message : "Error al crear turno");
    } finally {
      setCreando(false);
    }
  };

  if (clienteSeleccionado || (sinUsuario && sinUsuarioStep === "calendar")) {
    return (
      <CommonLayout>
        <div className={styles.page}>
          <button
            className={styles.btnVolver}
            onClick={() => {
              setClienteSeleccionado(null);
              setSinUsuario(false);
              setSinUsuarioStep("name");
              setNombreSinUsuario("");
              setFechaSeleccionada("");
              setSlotSeleccionado("");
            }}
          >
            ← Volver
          </button>

          {agendaDetail && (
            <div className={styles.contenido}>
              <section className={styles.card}>
                <h2 className={styles.sectionTitle}>
                  Turno para{" "}
                  {sinUsuario
                    ? nombreSinUsuario || "sin usuario"
                    : `${clienteSeleccionado!.nombre} ${clienteSeleccionado!.apellido}`}
                </h2>
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
                                setMsg("");
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
                            setMsg("");
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
                        Turno: <strong>{formatFecha(fechaSeleccionada)}</strong> a las{" "}
                        <strong>{slotSeleccionado}hs</strong>— <strong>{nombreCliente}</strong>
                      </p>
                      {msg && <p className={styles.errorMsg}>{msg}</p>}
                      <button className={styles.btnReservar} onClick={crearTurno} disabled={creando}>
                        {creando ? "Creando..." : "Confirmar turno"}
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
  }

  return (
    <CommonLayout>
      <div className={searchStyles.page}>
        <div className={searchStyles.topBar}>
          <button className={searchStyles.btnVolver} onClick={() => navigate("/agenda")}>
            ← Volver
          </button>
          <span className={searchStyles.totalCount}>
            {buscando ? "Buscando..." : `${clientes.length} cliente${clientes.length !== 1 ? "s" : ""}`}
          </span>
        </div>

        <div className={searchStyles.filterBar}>
          <div className={searchStyles.filterGroup}>
            <label className={searchStyles.filterLabel}>Nombre</label>
            <input
              className={searchStyles.filterInput}
              type="text"
              placeholder="Buscar cliente..."
              value={busqueda}
              onChange={(e) => {
                setBusqueda(e.target.value);
                setSinUsuario(false);
              }}
            />
          </div>
        </div>

        <button
          className={searchStyles.card}
          style={{ flexDirection: "row", gap: "0.75rem", cursor: "pointer", marginBottom: "0.25rem" }}
          onClick={() => setSinUsuario(true)}
        >
          <span className={searchStyles.avatar}>+</span>
          <div style={{ textAlign: "left" }}>
            <div className={searchStyles.cardName}>Sin usuario</div>
            <div className={searchStyles.cardInfo}>Crear turno sin cuenta de usuario</div>
          </div>
        </button>

        {loading ? (
          <p className={searchStyles.loadingHint}>Cargando agenda...</p>
        ) : error ? (
          <p className={styles.errorMsg}>{error}</p>
        ) : busqueda.trim().length < 2 ? (
          <p className={searchStyles.loadingHint}>Escribí al menos 2 caracteres para buscar clientes.</p>
        ) : clientes.length === 0 ? (
          <p className={searchStyles.loadingHint}>No se encontraron clientes.</p>
        ) : (
          <div className={searchStyles.grid}>
            {clientes.map((c) => (
              <div key={c.id} className={searchStyles.card} onClick={() => setClienteSeleccionado(c)}>
                <Avatar src={c.fotoPerfil} nombre={c.nombre} apellido={c.apellido} size={48} />
                <div className={searchStyles.cardName}>
                  {c.nombre} {c.apellido}
                </div>
                <div className={searchStyles.cardInfo}>{c.email}</div>
              </div>
            ))}
          </div>
        )}

        {sinUsuario && (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", padding: "1rem 0" }}>
            <div className={searchStyles.filterGroup}>
              <label className={searchStyles.filterLabel}>Nombre de la persona</label>
              <input
                className={searchStyles.filterInput}
                type="text"
                placeholder="Ej: Juan Pérez"
                value={nombreSinUsuario}
                onChange={(e) => setNombreSinUsuario(e.target.value)}
                autoFocus
              />
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button
                className={searchStyles.btnVolver}
                onClick={() => {
                  setSinUsuario(false);
                  setNombreSinUsuario("");
                }}
              >
                Cancelar
              </button>
              <button
                className={styles.btnReservar}
                disabled={!nombreSinUsuario.trim()}
                onClick={() => {
                  if (nombreSinUsuario.trim()) {
                    setSinUsuarioStep("calendar");
                    setBusqueda("");
                    setClientes([]);
                  }
                }}
              >
                Continuar
              </button>
            </div>
          </div>
        )}
      </div>
    </CommonLayout>
  );
};
