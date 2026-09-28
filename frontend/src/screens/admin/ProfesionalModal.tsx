import { useEffect, useState } from "react";
import styles from "../AdminScreen.module.css";
import { BASE_API_URL } from "@/config/app-query-client";
import { COLORS, DIAS_ORDEN, DIAS_LABEL, SECTOR_LABEL } from "../../constants";
import { formatFecha } from "../../utils/formatters";
import { Avatar } from "../../components/Avatar/Avatar";
import { DiaSemana } from "../../constants";
import {
  emptyRangos,
  agruparRangosPorDia,
  RangoDTO,
} from "../../utils/calendario";
import { UserAdminDTO, AgendaDetailDTO, AgendaPublicaDetailDTO, TurnoDTO } from "./types";

type ProfesionalModalProps = {
  userId: number;
  onClose: () => void;
  onDeleted: () => void;
  authedFetch: (url: string, options?: RequestInit) => Promise<Response>;
};

export const ProfesionalModal = ({ userId, onClose, onDeleted, authedFetch }: ProfesionalModalProps) => {
  const [user, setUser] = useState<UserAdminDTO | null>(null);
  const [agendaDetail, setAgendaDetail] = useState<AgendaDetailDTO | null>(null);
  const [tick, setTick] = useState(0);
  const refresh = () => setTick((t) => t + 1);

  const [editando, setEditando] = useState(false);
  const [editNombre, setEditNombre] = useState("");
  const [editApellido, setEditApellido] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editMsg, setEditMsg] = useState("");

  const [bloqueMinutos, setBloqueMinutos] = useState(30);
  const [mesesAnticipacion, setMesesAnticipacion] = useState(1);
  const [rangosPorDia, setRangosPorDia] = useState<Record<DiaSemana, RangoDTO[]>>(emptyRangos);
  const [savingAgenda, setSavingAgenda] = useState(false);
  const [agendaMsg, setAgendaMsg] = useState("");
  const [turnosConflictivos, setTurnosConflictivos] = useState<TurnoDTO[]>([]);

  const [verifiedMsg, setVerifiedMsg] = useState("");

  const toggleVerified = async () => {
    if (!user) return;
    setVerifiedMsg("");
    const res = await authedFetch(`${BASE_API_URL}/admin/users/${userId}/verified`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ verified: !user.verified }),
    });
    if (res.ok) {
      setVerifiedMsg(!user.verified ? "Cuenta verificada" : "Verificación removida");
      refresh();
    } else {
      setVerifiedMsg("Error al actualizar");
    }
  };

  const [activeMsg, setActiveMsg] = useState("");

  const toggleActive = async () => {
    if (!user) return;
    setActiveMsg("");
    const res = await authedFetch(`${BASE_API_URL}/admin/users/${userId}/active`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ active: !user.active }),
    });
    if (res.ok) {
      setActiveMsg(user.active ? "Cuenta desactivada" : "Cuenta activada");
      refresh();
    } else {
      const data = await res.json().catch(() => ({}));
      setActiveMsg(data.message ?? "Error al actualizar");
    }
  };

  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteMsg, setDeleteMsg] = useState("");

  useEffect(() => {
    let active = true;
    (async () => {
      const [resUser, resAgenda] = await Promise.allSettled([
        authedFetch(`${BASE_API_URL}/admin/users/${userId}`),
        authedFetch(`${BASE_API_URL}/agenda/profesional/${userId}`),
      ]);
      if (!active) return;
      if (resUser.status === "fulfilled" && resUser.value.ok) {
        const u: UserAdminDTO = await resUser.value.json();
        setUser(u);
        setEditNombre(u.nombre);
        setEditApellido(u.apellido);
        setEditEmail(u.email);
      }
      if (resAgenda.status === "fulfilled" && resAgenda.value.ok) {
        const agendaRes: AgendaPublicaDetailDTO = await resAgenda.value.json();
        setBloqueMinutos(agendaRes.agenda.bloqueMinutos);
        setMesesAnticipacion(agendaRes.agenda.mesesAnticipacion);
        setRangosPorDia(agruparRangosPorDia(agendaRes.agenda.rangos));
        if (agendaRes.agenda.id) {
          const resDetail = await authedFetch(`${BASE_API_URL}/agenda/${agendaRes.agenda.id}`);
          if (resDetail.ok) setAgendaDetail(await resDetail.json());
        }
      }
    })();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId, tick]);

  const guardarDatos = async () => {
    setEditMsg("");
    const res = await authedFetch(`${BASE_API_URL}/admin/users/${userId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nombre: editNombre, apellido: editApellido, email: editEmail }),
    });
    if (res.ok) {
      setEditMsg("Datos actualizados");
      setEditando(false);
      refresh();
    } else {
      const err = await res.json().catch(() => ({}));
      setEditMsg(err.error ?? "Error al actualizar");
    }
  };

  const guardarAgenda = async (accion?: "cancelar" | "mantener") => {
    if (!agendaDetail) return;
    setSavingAgenda(true);
    setAgendaMsg("");
    const rangos: RangoDTO[] = DIAS_ORDEN.flatMap((dia) => rangosPorDia[dia]);
    const url = accion
      ? `${BASE_API_URL}/agenda/${agendaDetail.agenda.id}?conflictos=${accion}`
      : `${BASE_API_URL}/agenda/${agendaDetail.agenda.id}`;
    try {
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
      setAgendaMsg("Agenda guardada");
      refresh();
    } catch (e: unknown) {
      setAgendaMsg(e instanceof Error ? e.message : "Error al guardar");
    } finally {
      setSavingAgenda(false);
    }
  };

  const eliminarUsuario = async () => {
    const res = await authedFetch(`${BASE_API_URL}/admin/users/${userId}`, {
      method: "DELETE",
    });
    if (res.ok || res.status === 204) onDeleted();
    else {
      const data = await res.json().catch(() => ({}));
      setDeleteMsg(data.message ?? "Error al eliminar el usuario");
    }
  };

  const agregarRango = (dia: DiaSemana) =>
    setRangosPorDia((prev) => ({ ...prev, [dia]: [...prev[dia], { id: 0, dia, horaInicio: "09:00", horaFin: "17:00" }] }));

  const eliminarRango = (dia: DiaSemana, idx: number) =>
    setRangosPorDia((prev) => ({ ...prev, [dia]: prev[dia].filter((_, i) => i !== idx) }));

  const editarRango = (dia: DiaSemana, idx: number, campo: "horaInicio" | "horaFin", valor: string) =>
    setRangosPorDia((prev) => {
      const copia = [...prev[dia]];
      copia[idx] = { ...copia[idx], [campo]: valor };
      return { ...prev, [dia]: copia };
    });

  if (!user)
    return (
      <div className={styles.modalOverlay}>
        <div className={styles.modal}>
          <button className={styles.btnClose} onClick={onClose}>
            ← Volver
          </button>
          <p className={styles.hint}>Cargando...</p>
        </div>
      </div>
    );

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <button className={styles.btnClose} onClick={onClose}>
          ← Volver al panel
        </button>

        <div className={styles.modalHeader}>
          <Avatar src={user.fotoPerfil} nombre={user.nombre} apellido={user.apellido} size={52} />
          <div>
            <h2 className={styles.modalTitle}>
              {user.nombre} {user.apellido}
            </h2>
            <p className={styles.modalSubtitle}>{user.email}</p>
            {user.profesion && (
              <p className={styles.modalSubtitle}>
                {user.profesion} · {user.sector ? SECTOR_LABEL[user.sector] : ""}
              </p>
            )}
          </div>
        </div>

        <section className={styles.modalSection}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>Datos personales</h3>
            {!editando && (
              <button className={styles.btnSecondary} onClick={() => setEditando(true)}>
                Editar
              </button>
            )}
          </div>
          {editando ? (
            <div className={styles.formGrid}>
              <label>Nombre</label>
              <input className={styles.input} value={editNombre} onChange={(e) => setEditNombre(e.target.value)} />
              <label>Apellido</label>
              <input className={styles.input} value={editApellido} onChange={(e) => setEditApellido(e.target.value)} />
              <label>Email</label>
              <input
                className={styles.input}
                type="email"
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
              />
              {editMsg && (
                <p className={editMsg === "Datos actualizados" ? styles.successMsg : styles.errorMsg}>{editMsg}</p>
              )}
              <div className={styles.formActions}>
                <button className={styles.btnPrimary} onClick={guardarDatos}>
                  Guardar
                </button>
                <button
                  className={styles.btnSecondary}
                  onClick={() => {
                    setEditando(false);
                    setEditMsg("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            editMsg && <p className={styles.successMsg}>{editMsg}</p>
          )}
        </section>

        <section className={styles.modalSection}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>Verificación de cuenta</h3>
            <span
              style={{
                padding: "2px 10px",
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                background: user.verified ? COLORS.lightGreen : COLORS.lightAmber,
                color: user.verified ? COLORS.success : COLORS.amberDark,
              }}
            >
              {user.verified ? "Verificada" : "Sin verificar"}
            </span>
          </div>
          {verifiedMsg && (
            <p className={verifiedMsg.includes("Error") ? styles.errorMsg : styles.successMsg}>{verifiedMsg}</p>
          )}
          <button className={user.verified ? styles.btnSecondary : styles.btnPrimary} onClick={toggleVerified}>
            {user.verified ? "Quitar verificación" : "Marcar como verificado"}
          </button>
        </section>

        {agendaDetail && (
          <section className={styles.modalSection}>
            <h3 className={styles.sectionTitle}>Agenda</h3>
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
            {agendaMsg && (
              <p className={agendaMsg === "Agenda guardada" ? styles.successMsg : styles.errorMsg}>{agendaMsg}</p>
            )}
            <button className={styles.btnPrimary} onClick={() => guardarAgenda()} disabled={savingAgenda}>
              {savingAgenda ? "Guardando..." : "Guardar agenda"}
            </button>
          </section>
        )}

        <section className={styles.modalSection}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>Estado de cuenta</h3>
            <span
              style={{
                padding: "2px 10px",
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                background: user.active ? COLORS.lightGreen : COLORS.lightAmber,
                color: user.active ? COLORS.success : COLORS.amberDark,
              }}
            >
              {user.active ? "Activa" : "Desactivada"}
            </span>
          </div>
          {activeMsg && (
            <p className={activeMsg.includes("Error") || activeMsg.includes("turnos") ? styles.errorMsg : styles.successMsg}>
              {activeMsg}
            </p>
          )}
          <button className={user.active ? styles.btnSecondary : styles.btnPrimary} onClick={toggleActive}>
            {user.active ? "Desactivar cuenta" : "Activar cuenta"}
          </button>
        </section>

        <section className={styles.modalSection}>
          {!confirmDelete ? (
            <button className={styles.btnDanger} onClick={() => setConfirmDelete(true)}>
              Eliminar usuario
            </button>
          ) : (
            <div className={styles.confirmDelete}>
              <p className={styles.warningText}>
                ¿Eliminar a {user.nombre} {user.apellido}? Esta acción no se puede deshacer. No se puede eliminar si tiene turnos pendientes.
              </p>
              {deleteMsg && <p className={styles.errorMsg}>{deleteMsg}</p>}
              <div className={styles.formActions}>
                <button className={styles.btnDanger} onClick={eliminarUsuario}>
                  Sí, eliminar
                </button>
                <button className={styles.btnSecondary} onClick={() => setConfirmDelete(false)}>
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </section>
      </div>

      {turnosConflictivos.length > 0 && (
        <div className={styles.subModalOverlay}>
          <div className={styles.subModal}>
            <h3 className={styles.modalTitle}>Turnos afectados por el cambio</h3>
            <p className={styles.hint}>Los siguientes turnos quedan fuera del nuevo horario.</p>
            <ul className={styles.turnosList}>
              {turnosConflictivos.map((t) => (
                <li key={t.id} className={styles.turnoItem}>
                  <strong>
                    {t.clienteNombre ?? t.nombreCliente ?? "—"} {t.clienteApellido ?? ""}
                  </strong>
                  {" — "}
                  {formatFecha(t.fecha)} {t.bloqueHorario}hs
                </li>
              ))}
            </ul>
            <div className={styles.formActions}>
              <button
                className={styles.btnDanger}
                onClick={() => {
                  setTurnosConflictivos([]);
                  guardarAgenda("cancelar");
                }}
                disabled={savingAgenda}
              >
                Cancelar turnos afectados
              </button>
              <button
                className={styles.btnSecondary}
                onClick={() => {
                  setTurnosConflictivos([]);
                  guardarAgenda("mantener");
                }}
                disabled={savingAgenda}
              >
                Mantener los turnos
              </button>
            </div>
            <button
              className={styles.btnSecondary}
              style={{ marginTop: "0.5rem" }}
              onClick={() => setTurnosConflictivos([])}
            >
              Volver sin guardar
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
