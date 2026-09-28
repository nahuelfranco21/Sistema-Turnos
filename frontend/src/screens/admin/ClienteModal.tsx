import { useEffect, useState } from "react";
import styles from "../AdminScreen.module.css";
import { BASE_API_URL } from "@/config/app-query-client";
import { COLORS, SECTORES, SECTOR_LABEL } from "../../constants";
import { formatFecha, labelEstado } from "../../utils/formatters";
import { generarSlots, diaSemanaDeISO } from "../../utils/calendario";
import { useDebounce } from "../../hooks/useDebounce";
import { Avatar } from "../../components/Avatar/Avatar";
import { UserAdminDTO, UserPublicDTO, TurnoDTO, AgendaPublicaDetailDTO } from "./types";

type ClienteModalProps = {
  userId: number;
  onClose: () => void;
  onDeleted: () => void;
  authedFetch: (url: string, options?: RequestInit) => Promise<Response>;
};

export const ClienteModal = ({ userId, onClose, onDeleted, authedFetch }: ClienteModalProps) => {
  const [user, setUser] = useState<UserAdminDTO | null>(null);
  const [turnos, setTurnos] = useState<TurnoDTO[]>([]);
  const [tick, setTick] = useState(0);
  const refresh = () => setTick((t) => t + 1);

  const [notifs, setNotifs] = useState<{ id: number; text: string; type: "success" | "error" }[]>([]);
  let notifId = 0;
  const notify = (text: string, type: "success" | "error") => {
    const id = ++notifId;
    setNotifs((prev) => [...prev, { id, text, type }]);
    setTimeout(() => setNotifs((prev) => prev.filter((n) => n.id !== id)), 4000);
  };

  const [editando, setEditando] = useState(false);
  const [editNombre, setEditNombre] = useState("");
  const [editApellido, setEditApellido] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editMsg, setEditMsg] = useState("");

  const [sacarTurnoOpen, setSacarTurnoOpen] = useState(false);
  const [busqProf, setBusqProf] = useState("");
  const [resultadosProf, setResultadosProf] = useState<UserPublicDTO[]>([]);
  const [buscandoProf, setBuscandoProf] = useState(false);
  const [profSeleccionado, setProfSeleccionado] = useState<UserPublicDTO | null>(null);
  const [agendaProf, setAgendaProf] = useState<AgendaPublicaDetailDTO | null>(null);
  const [fechaTurno, setFechaTurno] = useState("");
  const [slotTurno, setSlotTurno] = useState("");
  const [creandoTurno, setCreandoTurno] = useState(false);
  const [turnoMsg, setTurnoMsg] = useState("");

  const [editTurnoOpen, setEditTurnoOpen] = useState(false);
  const [editTurnoData, setEditTurnoData] = useState<TurnoDTO | null>(null);
  const [editTurnoFecha, setEditTurnoFecha] = useState("");
  const [editTurnoHora, setEditTurnoHora] = useState("");
  const [editTurnoSaving, setEditTurnoSaving] = useState(false);
  const [editTurnoMsg, setEditTurnoMsg] = useState("");
  const [editAgendaProf, setEditAgendaProf] = useState<AgendaPublicaDetailDTO | null>(null);
  const [editClienteBusqueda, setEditClienteBusqueda] = useState("");
  const [editResultadosClientes, setEditResultadosClientes] = useState<UserPublicDTO[]>([]);
  const [editBuscandoClientes, setEditBuscandoClientes] = useState(false);
  const [editClienteSeleccionado, setEditClienteSeleccionado] = useState<UserPublicDTO | null>(null);

  const [promoverOpen, setPromoverOpen] = useState(false);
  const [promNuevaProfesion, setPromNuevaProfesion] = useState("");
  const [promSector, setPromSector] = useState(SECTORES[0].value);
  const [promMsg, setPromMsg] = useState("");

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

  const hoy = new Date().toISOString().split("T")[0];

  useEffect(() => {
    let active = true;
    (async () => {
      const [resUser, resTurnos] = await Promise.allSettled([
        authedFetch(`${BASE_API_URL}/admin/users/${userId}`),
        authedFetch(`${BASE_API_URL}/admin/users/${userId}/turnos`),
      ]);
      if (!active) return;
      if (resUser.status === "fulfilled" && resUser.value.ok) {
        const u: UserAdminDTO = await resUser.value.json();
        setUser(u);
        setEditNombre(u.nombre);
        setEditApellido(u.apellido);
        setEditEmail(u.email);
      }
      if (resTurnos.status === "fulfilled" && resTurnos.value.ok) {
        setTurnos(await resTurnos.value.json());
      }
    })();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId, tick]);

  const debouncedBusqProf = useDebounce(busqProf, 300);

  useEffect(() => {
    if (debouncedBusqProf.trim().length < 2) {
      setResultadosProf([]);
      return;
    }
    let active = true;
    (async () => {
      setBuscandoProf(true);
      try {
        const res = await authedFetch(`${BASE_API_URL}/users?role=PROFESIONAL&q=${encodeURIComponent(debouncedBusqProf)}`);
        if (active && res.ok) setResultadosProf(await res.json());
      } finally {
        if (active) setBuscandoProf(false);
      }
    })();
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedBusqProf]);

  useEffect(() => {
    if (!profSeleccionado) {
      setAgendaProf(null);
      return;
    }
    (async () => {
      const res = await authedFetch(`${BASE_API_URL}/agenda/profesional/${profSeleccionado.id}`);
      if (res.ok) setAgendaProf(await res.json());
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profSeleccionado]);

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
      notify("Datos actualizados", "success");
    } else {
      const err = await res.json().catch(() => ({}));
      setEditMsg(err.error ?? "Error al actualizar");
    }
  };

  const crearTurno = async () => {
    if (!profSeleccionado || !agendaProf || !fechaTurno || !slotTurno) return;
    setCreandoTurno(true);
    setTurnoMsg("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          agendaId: agendaProf.agenda.id,
          clienteId: userId,
          fecha: fechaTurno,
          bloqueHorario: slotTurno,
        }),
      });
      if (res.status === 409) {
        setTurnoMsg("Ese bloque ya está ocupado");
        return;
      }
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setTurnoMsg(err.message ?? `Error ${res.status}`);
        return;
      }
      setTurnoMsg("Turno creado");
      notify("Turno creado correctamente", "success");
      setSacarTurnoOpen(false);
      setProfSeleccionado(null);
      setBusqProf("");
      setFechaTurno("");
      setSlotTurno("");
      refresh();
    } catch (e: unknown) {
      setTurnoMsg(e instanceof Error ? e.message : "Error al crear turno");
    } finally {
      setCreandoTurno(false);
    }
  };

  const cancelarTurno = async (turnoId: number) => {
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estado: "CANCELADO" }),
      });
      if (res.ok) {
        notify("Turno cancelado", "success");
        refresh();
      }
    } catch {
      /* silencioso */
    }
  };

  const promoverUsuario = async () => {
    setPromMsg("");
    const res = await authedFetch(`${BASE_API_URL}/admin/users/${userId}/role`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ profesion: promNuevaProfesion, sector: promSector }),
    });
    if (res.ok) {
      setPromMsg("Usuario promovido a profesional");
      setPromoverOpen(false);
      refresh();
      notify("Usuario promovido a profesional", "success");
    } else {
      const err = await res.json().catch(() => ({}));
      setPromMsg(err.error ?? "Error al promover");
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

  const debouncedEditCliente = useDebounce(editClienteBusqueda, 300);

  useEffect(() => {
    if (debouncedEditCliente.trim().length < 2) {
      setEditResultadosClientes([]);
      return;
    }
    let active = true;
    (async () => {
      setEditBuscandoClientes(true);
      try {
        const res = await authedFetch(
          `${BASE_API_URL}/users?role=CLIENTE&q=${encodeURIComponent(debouncedEditCliente)}`,
        );
        if (active && res.ok) setEditResultadosClientes(await res.json());
      } finally {
        if (active) setEditBuscandoClientes(false);
      }
    })();
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedEditCliente]);

  const abrirEditTurno = (t: TurnoDTO) => {
    setEditTurnoData(t);
    setEditTurnoFecha(t.fecha);
    setEditTurnoHora(t.bloqueHorario);
    setEditClienteBusqueda(t.clienteId ? `${t.clienteNombre ?? ""} ${t.clienteApellido ?? ""}` : "");
    setEditClienteSeleccionado(
      t.clienteId
        ? {
            id: t.clienteId,
            nombre: t.clienteNombre ?? "",
            apellido: t.clienteApellido ?? "",
            email: "",
            fotoPerfil: null,
          }
        : null,
    );
    setEditTurnoMsg("");
    setEditTurnoOpen(true);
    setEditAgendaProf(null);
    (async () => {
      if (!t.profesionalId) return;
      const res = await authedFetch(`${BASE_API_URL}/agenda/profesional/${t.profesionalId}`);
      if (res.ok) setEditAgendaProf(await res.json());
    })();
  };

  const guardarEditTurno = async () => {
    if (!editTurnoData || !editTurnoFecha || !editTurnoHora) return;
    setEditTurnoSaving(true);
    setEditTurnoMsg("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${editTurnoData.id}/modificar`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fecha: editTurnoFecha,
          bloqueHorario: editTurnoHora,
          clienteId: editClienteSeleccionado?.id ?? null,
        }),
      });
      if (res.status === 409) {
        setEditTurnoMsg("Ese bloque ya está ocupado");
        return;
      }
      if (res.status === 400) {
        const err = await res.json().catch(() => ({}));
        setEditTurnoMsg(err.error ?? err.message ?? "Fecha u hora inválida");
        return;
      }
      if (!res.ok) throw new Error(`Error ${res.status}`);
      notify("Turno modificado correctamente", "success");
      setEditTurnoOpen(false);
      refresh();
    } catch (e: unknown) {
      setEditTurnoMsg(e instanceof Error ? e.message : "Error al modificar turno");
    } finally {
      setEditTurnoSaving(false);
    }
  };

  const slotsDisponibles = (() => {
    if (!agendaProf || !fechaTurno) return [];
    const dia = diaSemanaDeISO(fechaTurno);
    const todos = generarSlots(agendaProf.agenda.rangos, agendaProf.agenda.bloqueMinutos, dia);
    const ocupados = new Set(agendaProf.turnos.filter((t) => t.fecha === fechaTurno).map((t) => t.bloqueHorario));
    return todos.filter((s) => !ocupados.has(s));
  })();

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

  const proximosTurnos = turnos
    .filter((t) => t.fecha >= hoy && t.estado !== "CANCELADO")
    .sort((a, b) => a.fecha.localeCompare(b.fecha));

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal} style={{ maxWidth: "680px" }}>
        <div
          style={{
            position: "fixed",
            top: "1rem",
            right: "1rem",
            zIndex: 300,
            display: "flex",
            flexDirection: "column",
            gap: "0.5rem",
            maxWidth: "360px",
          }}
        >
          {notifs.map((n) => (
            <div
              key={n.id}
              style={{
                padding: "0.75rem 1rem",
                borderRadius: "10px",
                fontSize: "0.9rem",
                fontWeight: 500,
                boxShadow: "0 4px 12px rgba(0,0,0,0.2)",
                background: n.type === "success" ? COLORS.success : COLORS.error,
                color: COLORS.white,
              }}
            >
              {n.text}
            </div>
          ))}
        </div>

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

        <section className={styles.modalSection}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>Turnos próximos</h3>
            <button
              className={styles.btnSecondary}
              onClick={() => {
                setSacarTurnoOpen(true);
                setTurnoMsg("");
              }}
            >
              + Sacar turno
            </button>
          </div>
          {turnoMsg && <p className={turnoMsg === "Turno creado" ? styles.successMsg : styles.errorMsg}>{turnoMsg}</p>}
          {proximosTurnos.length === 0 ? (
            <p className={styles.hint}>No tiene turnos próximos</p>
          ) : (
            <ul className={styles.turnosList}>
              {proximosTurnos.map((t) => (
                <li key={t.id} className={styles.turnoItem}>
                  <div style={{ flex: 1 }}>
                    <div className={styles.turnoFecha}>
                      {t.profesionalNombre && (
                        <span>
                          {t.profesionalNombre} {t.profesionalApellido} —{" "}
                        </span>
                      )}
                      {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                    </div>
                    <div className={`${styles.turnoEstado} ${styles[`estado${t.estado}`]}`}>
                      {labelEstado(t.estado)}
                    </div>
                  </div>
                  <button
                    className={styles.btnPrimary}
                    style={{ padding: "0.3rem 0.65rem", fontSize: "0.8rem" }}
                    onClick={() => abrirEditTurno(t)}
                  >
                    Editar
                  </button>
                  <button className={styles.btnSmall} onClick={() => cancelarTurno(t.id)}>
                    Cancelar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {(() => {
          const historial = turnos
            .filter((t) => t.fecha < hoy || t.estado === "CANCELADO")
            .sort((a, b) => b.fecha.localeCompare(a.fecha))
            .slice(0, 10);
          if (historial.length === 0) return null;
          return (
            <section className={styles.modalSection}>
              <h3 className={styles.sectionTitle}>Historial de turnos</h3>
              <ul className={styles.turnosList}>
                {historial.map((t) => (
                  <li key={t.id} className={styles.turnoItem}>
                    <div style={{ flex: 1 }}>
                      <div className={styles.turnoFecha}>
                        {t.profesionalNombre && (
                          <span>
                            {t.profesionalNombre} {t.profesionalApellido} —{" "}
                          </span>
                        )}
                        {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                      </div>
                      <div className={`${styles.turnoEstado} ${styles[`estado${t.estado}`]}`}>
                        {labelEstado(t.estado)}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          );
        })()}

        <section className={styles.modalSection}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>Promover a profesional</h3>
            {!promoverOpen && (
              <button className={styles.btnSecondary} onClick={() => setPromoverOpen(true)}>
                Promover
              </button>
            )}
          </div>
          {promMsg && <p className={promMsg.includes("promovido") ? styles.successMsg : styles.errorMsg}>{promMsg}</p>}
          {promoverOpen && (
            <div className={styles.formGrid}>
              <label>Profesión</label>
              <input
                className={styles.input}
                value={promNuevaProfesion}
                onChange={(e) => setPromNuevaProfesion(e.target.value)}
                placeholder="Ej: Médico clínico"
              />
              <label>Sector</label>
              <select className={styles.input} value={promSector} onChange={(e) => setPromSector(e.target.value)}>
                {SECTORES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {SECTOR_LABEL[s.value]}
                  </option>
                ))}
              </select>
              <div className={styles.formActions}>
                <button className={styles.btnPrimary} onClick={promoverUsuario} disabled={!promNuevaProfesion.trim()}>
                  Confirmar promoción
                </button>
                <button
                  className={styles.btnSecondary}
                  onClick={() => {
                    setPromoverOpen(false);
                    setPromMsg("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </section>

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

      {sacarTurnoOpen && (
        <div className={styles.subModalOverlay}>
          <div className={styles.subModal}>
            <button
              className={styles.btnClose}
              onClick={() => {
                setSacarTurnoOpen(false);
                setProfSeleccionado(null);
                setBusqProf("");
              }}
            >
              ← Volver
            </button>
            <h3 className={styles.modalTitle}>
              Sacar turno para {user.nombre} {user.apellido}
            </h3>

            {!profSeleccionado ? (
              <div className={styles.searchWrapper}>
                <input
                  className={styles.searchInput}
                  type="text"
                  placeholder="🔍 Buscar profesional..."
                  value={busqProf}
                  onChange={(e) => setBusqProf(e.target.value)}
                />
                {buscandoProf && <p className={styles.hint}>Buscando...</p>}
                {resultadosProf.length > 0 && (
                  <ul className={styles.dropdown}>
                    {resultadosProf.map((p) => (
                      <li
                        key={p.id}
                        className={styles.dropdownItem}
                        onClick={() => {
                          setProfSeleccionado(p);
                          setBusqProf(`${p.nombre} ${p.apellido}`);
                          setResultadosProf([]);
                        }}
                      >
                        <Avatar src={p.fotoPerfil} nombre={p.nombre} apellido={p.apellido} size={32} />
                        {p.nombre} {p.apellido}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ) : (
              <div>
                <p className={styles.hint}>
                  Profesional:{" "}
                  <strong>
                    {profSeleccionado.nombre} {profSeleccionado.apellido}
                  </strong>
                  <button
                    className={styles.btnLink}
                    onClick={() => {
                      setProfSeleccionado(null);
                      setBusqProf("");
                      setFechaTurno("");
                      setSlotTurno("");
                    }}
                  >
                    Cambiar
                  </button>
                </p>

                <div className={styles.formRow}>
                  <label>Fecha</label>
                  <input
                    type="date"
                    className={styles.inputSmall}
                    value={fechaTurno}
                    min={hoy}
                    onChange={(e) => {
                      setFechaTurno(e.target.value);
                      setSlotTurno("");
                    }}
                  />
                </div>

                {fechaTurno && slotsDisponibles.length === 0 && (
                  <p className={styles.hint}>No hay horarios disponibles ese día</p>
                )}

                {slotsDisponibles.length > 0 && (
                  <div className={styles.slotsGrid}>
                    {slotsDisponibles.map((s) => (
                      <button
                        key={s}
                        className={`${styles.slotBtn} ${slotTurno === s ? styles.slotBtnSelected : ""}`}
                        onClick={() => setSlotTurno(s)}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                )}

                {turnoMsg && (
                  <p className={turnoMsg === "Turno creado" ? styles.successMsg : styles.errorMsg}>{turnoMsg}</p>
                )}

                <button
                  className={styles.btnPrimary}
                  onClick={crearTurno}
                  disabled={creandoTurno || !fechaTurno || !slotTurno}
                >
                  {creandoTurno ? "Creando..." : "Confirmar turno"}
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {editTurnoOpen && editTurnoData && (
        <div className={styles.subModalOverlay}>
          <div className={styles.subModal}>
            <button className={styles.btnClose} onClick={() => setEditTurnoOpen(false)}>
              ← Volver
            </button>
            <h3 className={styles.modalTitle}>Modificar turno</h3>
            <p className={styles.hint}>
              Turno actual: {formatFecha(editTurnoData.fecha)} {editTurnoData.bloqueHorario}hs
              {editTurnoData.profesionalNombre && (
                <>
                  {" "}
                  — {editTurnoData.profesionalNombre} {editTurnoData.profesionalApellido}
                </>
              )}
            </p>

            <div className={styles.formRow}>
              <label>Nueva fecha</label>
              <input
                type="date"
                className={styles.inputSmall}
                value={editTurnoFecha}
                min={hoy}
                onChange={(e) => {
                  setEditTurnoFecha(e.target.value);
                  setEditTurnoHora("");
                }}
              />
            </div>
            {editTurnoFecha &&
              editAgendaProf &&
              (() => {
                const dia = diaSemanaDeISO(editTurnoFecha);
                const todos = generarSlots(editAgendaProf.agenda.rangos, editAgendaProf.agenda.bloqueMinutos, dia);
                const ocupados = new Set(
                  editAgendaProf.turnos
                    .filter((t) => t.fecha === editTurnoFecha && t.id !== editTurnoData?.id)
                    .map((t) => t.bloqueHorario),
                );
                const disponibles = todos.filter((s) => !ocupados.has(s));
                return disponibles.length === 0 ? (
                  <p className={styles.hint}>No hay horarios disponibles ese día</p>
                ) : (
                  <div className={styles.slotsGrid}>
                    {disponibles.map((s) => (
                      <button
                        key={s}
                        className={`${styles.slotBtn} ${editTurnoHora === s ? styles.slotBtnSelected : ""}`}
                        onClick={() => setEditTurnoHora(s)}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                );
              })()}

            <div className={styles.searchWrapper}>
              <label className={styles.formRow} style={{ marginBottom: "0.25rem" }}>
                Cambiar cliente (opcional)
              </label>
              <input
                className={styles.searchInput}
                type="text"
                placeholder="🔍 Buscar nuevo cliente..."
                value={editClienteBusqueda}
                onChange={(e) => {
                  setEditClienteBusqueda(e.target.value);
                  setEditClienteSeleccionado(null);
                }}
              />
              {editBuscandoClientes && <p className={styles.hint}>Buscando...</p>}
              {editResultadosClientes.length > 0 && !editClienteSeleccionado && (
                <ul className={styles.dropdown}>
                  {editResultadosClientes.map((u) => (
                    <li
                      key={u.id}
                      className={styles.dropdownItem}
                      onClick={() => {
                        setEditClienteSeleccionado(u);
                        setEditClienteBusqueda(`${u.nombre} ${u.apellido}`);
                        setEditResultadosClientes([]);
                      }}
                    >
                      <Avatar src={u.fotoPerfil} nombre={u.nombre} apellido={u.apellido} size={32} />
                      {u.nombre} {u.apellido} — {u.email}
                    </li>
                  ))}
                </ul>
              )}
              {editClienteSeleccionado && (
                <p className={styles.hint}>
                  Cliente: <strong>{editClienteSeleccionado.nombre} {editClienteSeleccionado.apellido}</strong>
                </p>
              )}
            </div>

            {editTurnoMsg && <p className={styles.errorMsg}>{editTurnoMsg}</p>}
            <div className={styles.formActions}>
              <button
                className={styles.btnPrimary}
                onClick={guardarEditTurno}
                disabled={editTurnoSaving || !editTurnoFecha || !editTurnoHora}
              >
                {editTurnoSaving ? "Guardando..." : "Guardar cambios"}
              </button>
              <button className={styles.btnSecondary} onClick={() => setEditTurnoOpen(false)}>
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
