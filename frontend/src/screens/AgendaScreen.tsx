import { useEffect, useRef, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./AgendaScreen.module.css";

import { DiaSemana, DIAS_ORDEN, DIAS_LABEL } from "../constants";
import { formatFecha, labelEstado } from "../utils/formatters";
import { Avatar } from "../components/Avatar/Avatar";
import { RangoDTO, emptyRangos, agruparRangosPorDia } from "../utils/calendario";

import { CollapsibleSection } from "./agenda/CollapsibleSection";
import { useNotification } from "./agenda/useNotification";
import { CancelTurnoModal } from "./agenda/CancelTurnoModal";
import { RestituirTurnoModal } from "./agenda/RestituirTurnoModal";
import { ConflictosModal } from "./agenda/ConflictosModal";
import { BulkCancelModal } from "./agenda/BulkCancelModal";

type AgendaDTO = {
  id: number;
  profesionalId: number;
  bloqueMinutos: number;
  mesesAnticipacion: number;
  rangos: RangoDTO[];
};
type TurnoDTO = {
  id: number;
  agendaId: number;
  clienteId: number | null;
  clienteNombre: string | null;
  clienteApellido: string | null;
  nombreCliente: string | null;
  clienteFotoPerfil: string | null;
  servicioId: number | null;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};
type AgendaDetailDTO = { agenda: AgendaDTO; turnos: TurnoDTO[] };

export const AgendaScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();
  const { notifs, notify } = useNotification();

  const [tick, setTick] = useState(0);
  const refresh = () => setTick((t) => t + 1);

  const [agendaDetail, setAgendaDetail] = useState<AgendaDetailDTO | null>(null);
  const [loadingAgenda, setLoadingAgenda] = useState(true);
  const [errorAgenda, setErrorAgenda] = useState("");

  const [serviciosCount, setServiciosCount] = useState(0);

  const [bloqueMinutos, setBloqueMinutos] = useState(30);
  const [rangosPorDia, setRangosPorDia] = useState<Record<DiaSemana, RangoDTO[]>>(emptyRangos);
  const [mesesAnticipacion, setMesesAnticipacion] = useState(1);
  const [savingAgenda, setSavingAgenda] = useState(false);
  const [saveMsg, setSaveMsg] = useState("");

  const [turnosConflictivos, setTurnosConflictivos] = useState<TurnoDTO[]>([]);

  const [bulkFecha, setBulkFecha] = useState("");
  const [bulkCanceling, setBulkCanceling] = useState(false);
  const [bulkConfirmOpen, setBulkConfirmOpen] = useState(false);

  const [agendaSectionOpen, setAgendaSectionOpen] = useState(false);
  const agendaEditorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    setLoadingAgenda(true);
    setErrorAgenda("");

    (async () => {
      try {
        const res = await authedFetch(`${BASE_API_URL}/agenda/mi-agenda`);
        if (!active) return;
        if (!res.ok) throw new Error(`Error ${res.status}`);
        const data: AgendaDetailDTO = await res.json();
        setAgendaDetail(data);
        setBloqueMinutos(data.agenda.bloqueMinutos);
        setMesesAnticipacion(data.agenda.mesesAnticipacion ?? 1);
        setRangosPorDia(agruparRangosPorDia(data.agenda.rangos));
        const resServicios = await authedFetch(`${BASE_API_URL}/servicio/agenda/${data.agenda.id}`);
        if (active && resServicios.ok) {
          const servicios = await resServicios.json();
          setServiciosCount(servicios.length);
        }
      } catch (e: unknown) {
        if (active) setErrorAgenda(e instanceof Error ? e.message : "Error al cargar la agenda");
      } finally {
        if (active) setLoadingAgenda(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [tick]);

  const guardarAgenda = async (accion?: "cancelar" | "mantener") => {
    if (!agendaDetail) {
      setSaveMsg("La agenda no está cargada aún, intentá de nuevo");
      return;
    }
    setSavingAgenda(true);
    setSaveMsg("");
    try {
      const rangos: RangoDTO[] = DIAS_ORDEN.flatMap((dia) => rangosPorDia[dia]);
      const url = accion
        ? `${BASE_API_URL}/agenda/${agendaDetail.agenda.id}?conflictos=${accion}`
        : `${BASE_API_URL}/agenda/${agendaDetail.agenda.id}`;
      const res = await authedFetch(url, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bloqueMinutos, mesesAnticipacion, rangos }),
      });
      if (res.status === 409) {
        const data = await res.json();
        setTurnosConflictivos(data.turnosConflictivos ?? []);
        return;
      }
      if (!res.ok) throw new Error(`Error ${res.status}`);
      setTurnosConflictivos([]);
      setSaveMsg("Agenda guardada");
      notify("Agenda guardada correctamente", "success");
      refresh();
    } catch (e: unknown) {
      setSaveMsg(e instanceof Error ? e.message : "Error al guardar");
      notify(e instanceof Error ? e.message : "Error al guardar la agenda", "error");
    } finally {
      setSavingAgenda(false);
    }
  };

  const resolverConflictos = async (accion: "cancelar" | "mantener") => {
    setTurnosConflictivos([]);
    await guardarAgenda(accion);
  };

  const agregarRango = (dia: DiaSemana) =>
    setRangosPorDia((prev) => ({ ...prev, [dia]: [...prev[dia], { dia, horaInicio: "09:00", horaFin: "17:00" }] }));

  const eliminarRango = (dia: DiaSemana, idx: number) =>
    setRangosPorDia((prev) => ({ ...prev, [dia]: prev[dia].filter((_, i) => i !== idx) }));

  const editarRango = (dia: DiaSemana, idx: number, campo: "horaInicio" | "horaFin", valor: string) =>
    setRangosPorDia((prev) => {
      const copia = [...prev[dia]];
      copia[idx] = { ...copia[idx], [campo]: valor };
      return { ...prev, [dia]: copia };
    });

  const [turnoACancelar, setTurnoACancelar] = useState<TurnoDTO | null>(null);
  const [turnoARestituir, setTurnoARestituir] = useState<TurnoDTO | null>(null);
  const [actionError, setActionError] = useState("");

  const cancelarTurno = async (turno: TurnoDTO) => {
    setActionError("");
    const estaPagado = turno.estado === "CONFIRMADO";
    try {
      if (estaPagado) {
        const res = await authedFetch(`${BASE_API_URL}/turno/${turno.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ estado: "CANCELADO" }),
        });
        if (res.ok) {
          notify("Turno cancelado. Se notificó al cliente por mail.", "success");
          refresh();
        } else setActionError("No se pudo cancelar el turno. Intentá de nuevo.");
      } else {
        const res = await authedFetch(`${BASE_API_URL}/turno/${turno.id}`, {
          method: "DELETE",
        });
        if (res.ok || res.status === 204) {
          notify("Turno eliminado", "success");
          refresh();
        } else setActionError("No se pudo eliminar el turno. Intentá de nuevo.");
      }
    } catch {
      setActionError("Error de conexión.");
    }
  };

  const restituirTurno = async (turnoId: number) => {
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`, {
        method: "DELETE",
      });
      if (res.ok || res.status === 204) {
        notify("Turno liberado, ya puede ser reservado", "success");
        refresh();
      }
    } catch {
      /* silencioso */
    }
  };

  const bloquearTurno = async (turno: TurnoDTO) => {
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turno.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estado: "DESHABILITADO" }),
      });
      if (res.ok) {
        notify("Turno bloqueado, no disponible para reservas", "success");
        refresh();
      }
    } catch {
      /* silencioso */
    }
  };

  const ejecutarBulkCancel = async () => {
    if (!agendaDetail || !bulkFecha) return;
    setBulkCanceling(true);
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/cancelar-en-lote`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ agendaId: agendaDetail.agenda.id, fecha: bulkFecha }),
      });
      if (!res.ok) throw new Error(`Error ${res.status}`);
      const cancelados = await res.json();
      const count = cancelados.length;
      notify(`${count} turno(s) cancelado(s) del día ${formatFecha(bulkFecha)}`, "success");
      setBulkConfirmOpen(false);
      setBulkFecha("");
      refresh();
    } catch (e: unknown) {
      notify(e instanceof Error ? e.message : "Error al cancelar turnos", "error");
    } finally {
      setBulkCanceling(false);
    }
  };

  const hoy = new Date().toISOString().split("T")[0];
  const proximosTurnos = (agendaDetail?.turnos ?? [])
    .filter((t) => t.fecha >= hoy && t.estado !== "CANCELADO" && (t.clienteId !== null || t.nombreCliente !== null))
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.bloqueHorario.localeCompare(b.bloqueHorario));

  const turnosCancelados = (agendaDetail?.turnos ?? [])
    .filter((t) => t.fecha >= hoy && t.estado === "CANCELADO")
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.bloqueHorario.localeCompare(b.bloqueHorario));

  const turnosSinCliente = (agendaDetail?.turnos ?? [])
    .filter((t) => t.fecha >= hoy && t.clienteId === null && t.estado !== "CANCELADO")
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.bloqueHorario.localeCompare(b.bloqueHorario));

  const rangosCount = agendaDetail?.agenda.rangos.length ?? 0;
  const setupDone = rangosCount > 0 && serviciosCount > 0;
  const setupItems = [
    { label: "Configurar días y horarios disponibles", done: rangosCount > 0, section: "agenda" as const },
    { label: "Agregar al menos un servicio", done: serviciosCount > 0, section: "servicios" as const },
  ];

  return (
    <CommonLayout>
      <div className={styles.notifContainer}>
        {notifs.map((n) => (
          <div
            key={n.id}
            className={`${styles.notif} ${n.type === "success" ? styles.notifSuccess : styles.notifError}`}
          >
            {n.text}
          </div>
        ))}
      </div>

      {!setupDone && !loadingAgenda && (
        <section className={styles.onboardingBanner}>
          <h2 className={styles.onboardingTitle}>Completá estos pasos para estar disponible</h2>
          <div className={styles.onboardingList}>
            {setupItems.map((item) => (
              <div key={item.label} className={styles.onboardingItem}>
                <span className={item.done ? styles.checkDone : styles.checkPending}>{item.done ? "✓" : "○"}</span>
                <span className={item.done ? styles.checkLabelDone : styles.checkLabelPending}>{item.label}</span>
                {!item.done && (
                  <button
                    className={styles.onboardingBtn}
                    onClick={() => {
                      if (item.section === "agenda") {
                        setAgendaSectionOpen(true);
                        agendaEditorRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                      } else navigate("/servicios");
                    }}
                  >
                    Ir
                  </button>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      <div className={styles.dashboard}>
        <div className={styles.leftCol}>
          <button className={styles.btnTurno} onClick={() => navigate("/crear-turno")}>
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="11" y1="8" x2="11" y2="14" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
            Crear turno
          </button>

          <CollapsibleSection title="Próximos turnos">
            {actionError && <p className={styles.errorMsg}>{actionError}</p>}
            {loadingAgenda ? (
              <p className={styles.hint}>Cargando...</p>
            ) : errorAgenda ? (
              <p className={styles.errorMsg}>{errorAgenda}</p>
            ) : proximosTurnos.length === 0 ? (
              <p className={styles.hint}>No hay turnos próximos</p>
            ) : (
              <ul className={styles.turnosList}>
                {proximosTurnos.map((t) => (
                  <li key={t.id} className={styles.turnoItem}>
                    <Avatar src={t.clienteFotoPerfil} nombre={t.clienteNombre ?? t.nombreCliente} apellido={t.clienteApellido} size={40} />
                    <div style={{ flex: 1 }}>
                      <div className={styles.turnoNombre}>
                        {t.clienteNombre ?? t.nombreCliente ?? "—"} {t.clienteApellido ?? ""}
                      </div>
                      <div className={styles.turnoFecha}>
                        {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                        {t.servicioNombre && (
                          <> · {t.servicioNombre} · ${t.servicioPrecio}</>
                        )}
                      </div>
                      <div className={`${styles.turnoEstado} ${styles[`estado${t.estado}`]}`}>
                        {labelEstado(t.estado)}
                      </div>
                    </div>
                    <button className={styles.btnCancelar} onClick={() => setTurnoACancelar(t)}>
                      Cancelar
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </CollapsibleSection>

          {turnosCancelados.length > 0 && (
            <CollapsibleSection title="Turnos cancelados" defaultOpen={false}>
              <ul className={styles.turnosList}>
                {turnosCancelados.map((t) => (
                  <li key={t.id} className={`${styles.turnoItem} ${styles.turnoItemCancelado}`}>
                    <Avatar src={t.clienteFotoPerfil} nombre={t.clienteNombre ?? t.nombreCliente} apellido={t.clienteApellido} size={40} />
                    <div style={{ flex: 1 }}>
                      <div className={styles.turnoNombre}>
                        {t.clienteNombre ?? t.nombreCliente ?? "—"} {t.clienteApellido ?? ""}
                      </div>
                      <div className={styles.turnoFecha}>
                        {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                      </div>
                      <div className={`${styles.turnoEstado} ${styles.estadoCANCELADO}`}>Cancelado</div>
                    </div>
                    <button className={styles.btnRestituir} onClick={() => setTurnoARestituir(t)}>
                      Gestionar
                    </button>
                  </li>
                ))}
              </ul>
            </CollapsibleSection>
          )}

          {turnosSinCliente.length > 0 && (
            <CollapsibleSection title="Bloques sin cliente" defaultOpen={false}>
              <ul className={styles.turnosList}>
                {turnosSinCliente.map((t) => (
                  <li key={t.id} className={styles.turnoItem}>
                    <div className={styles.avatar}>—</div>
                    <div style={{ flex: 1 }}>
                      <div className={styles.turnoNombre}>Sin cliente</div>
                      <div className={styles.turnoFecha}>
                        {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                      </div>
                      <div className={`${styles.turnoEstado} ${styles.estadoDESHABILITADO}`}>
                        {labelEstado(t.estado)}
                      </div>
                    </div>
                    <button className={styles.btnCancelar} onClick={() => setTurnoACancelar(t)}>
                      Eliminar
                    </button>
                  </li>
                ))}
              </ul>
            </CollapsibleSection>
          )}
        </div>

        <div className={styles.rightCol} ref={agendaEditorRef}>
          <button className={styles.btnCalendario} onClick={() => navigate("/calendario")}>
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            Ver calendario
          </button>

          <CollapsibleSection
            key={loadingAgenda ? "loading" : "loaded"}
            title="Modificar tu agenda"
            defaultOpen={rangosCount === 0}
            open={agendaSectionOpen}
            onToggle={setAgendaSectionOpen}
          >
            <div className={styles.formRow}>
              <label>Duración de cada turno</label>
              <div className={styles.inputGroup}>
                <input
                  type="number"
                  className={styles.inputSmall}
                  value={bloqueMinutos}
                  min={5}
                  step={5}
                  onChange={(e) => setBloqueMinutos(Number(e.target.value))}
                />
                <span>min</span>
              </div>
            </div>

            <div className={styles.formRow}>
              <label>Turnos hasta dentro de</label>
              <div className={styles.inputGroup}>
                <input
                  type="number"
                  className={styles.inputSmall}
                  value={mesesAnticipacion}
                  min={1}
                  max={12}
                  onChange={(e) => setMesesAnticipacion(Number(e.target.value))}
                />
                <span>mes(es)</span>
              </div>
            </div>

            <div className={styles.diasEditor}>
              {DIAS_ORDEN.map((dia) => (
                <div key={dia} className={styles.diaRow}>
                  <span className={styles.diaLabel}>{DIAS_LABEL[dia]}</span>
                  <div className={styles.rangosList}>
                    {rangosPorDia[dia].map((r, idx) => (
                      <div key={idx} className={styles.rangoChip}>
                        <input
                          type="time"
                          className={styles.timeInput}
                          value={r.horaInicio}
                          onChange={(e) => editarRango(dia, idx, "horaInicio", e.target.value)}
                        />
                        <span>–</span>
                        <input
                          type="time"
                          className={styles.timeInput}
                          value={r.horaFin}
                          onChange={(e) => editarRango(dia, idx, "horaFin", e.target.value)}
                        />
                        <button className={styles.btnRemove} onClick={() => eliminarRango(dia, idx)}>
                          ×
                        </button>
                      </div>
                    ))}
                    <button className={styles.btnAdd} onClick={() => agregarRango(dia)}>
                      +
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {saveMsg && (
              <p className={saveMsg === "Agenda guardada" ? styles.successMsg : styles.errorMsg}>{saveMsg}</p>
            )}
            <button className={styles.btnPrimary} onClick={() => guardarAgenda()} disabled={savingAgenda}>
              {savingAgenda ? "Guardando..." : "Guardar cambios"}
            </button>
          </CollapsibleSection>

          <CollapsibleSection title="Cancelación masiva por día" defaultOpen={false}>
            <p className={styles.hint}>Cancelá todos los turnos de una fecha específica.</p>
            <div className={styles.formRow}>
              <label>Fecha</label>
              <input
                type="date"
                className={styles.inputSmall}
                value={bulkFecha}
                min={hoy}
                onChange={(e) => setBulkFecha(e.target.value)}
              />
            </div>
            <button
              className={styles.btnDanger}
              onClick={() => {
                if (bulkFecha) setBulkConfirmOpen(true);
              }}
              disabled={!bulkFecha || !agendaDetail}
            >
              Cancelar todos los turnos del día
            </button>
          </CollapsibleSection>
        </div>
      </div>

      {turnoACancelar !== null && (
        <CancelTurnoModal
          turno={turnoACancelar}
          onCancel={cancelarTurno}
          onClose={() => setTurnoACancelar(null)}
        />
      )}

      {turnoARestituir !== null && (
        <RestituirTurnoModal
          turno={turnoARestituir}
          onRestituir={restituirTurno}
          onBloquear={async (t) => { await bloquearTurno(t as TurnoDTO); }}
          onClose={() => setTurnoARestituir(null)}
          authedFetch={authedFetch}
        />
      )}

      {turnosConflictivos.length > 0 && (
        <ConflictosModal
          turnos={turnosConflictivos}
          savingAgenda={savingAgenda}
          onCancelar={() => resolverConflictos("cancelar")}
          onMantener={() => resolverConflictos("mantener")}
        />
      )}

      {bulkConfirmOpen && (
        <BulkCancelModal
          bulkFecha={bulkFecha}
          bulkCanceling={bulkCanceling}
          onConfirm={ejecutarBulkCancel}
          onClose={() => setBulkConfirmOpen(false)}
        />
      )}
    </CommonLayout>
  );
};
