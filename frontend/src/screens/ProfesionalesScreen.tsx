import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./ProfesionalesScreen.module.css";

import { formatFecha, labelEstado } from "../utils/formatters";
import { Avatar } from "../components/Avatar/Avatar";

type TurnoDTO = {
  id: number;
  profesionalNombre: string;
  profesionalApellido: string;
  profesionalProfesion: string | null;
  profesionalSector: string | null;
  profesionalUbicacion: string | null;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  profesionalFotoPerfil: string | null;
  estado: string;
};

type ServicioDTO = {
  id: number;
  nombre: string;
  precio: number;
};

type ProfesionalDTO = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  profesion: string | null;
  sector: string | null;
  ubicacion: string | null;
  fotoPerfil: string | null;
  servicios: ServicioDTO[];
};

function formatProfesionalInfo(p: ProfesionalDTO): string {
  const parts: string[] = [];
  if (p.sector) parts.push(`Sector: ${p.sector}`);
  if (p.profesion) parts.push(`Profesión: ${p.profesion}`);
  if (p.ubicacion) parts.push(`Ubicación: ${p.ubicacion}`);
  return parts.join(" - ");
}

function formatProfesionalInfoFromTurno(t: TurnoDTO): string {
  const parts: string[] = [];
  if (t.profesionalSector) parts.push(`Sector: ${t.profesionalSector}`);
  if (t.profesionalProfesion) parts.push(`Profesión: ${t.profesionalProfesion}`);
  if (t.profesionalUbicacion) parts.push(`Ubicación: ${t.profesionalUbicacion}`);
  return parts.join(" - ");
}

export const ProfesionalesScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();

  const [tick, setTick] = useState(0);
  const refresh = () => setTick((t) => t + 1);

  const [turnos, setTurnos] = useState<TurnoDTO[]>([]);
  const [loadingTurnos, setLoadingTurnos] = useState(true);

  const [recientes, setRecientes] = useState<ProfesionalDTO[]>([]);
  const [loadingRecientes, setLoadingRecientes] = useState(true);

  useEffect(() => {
    let active = true;

    async function cargar() {
      setLoadingTurnos(true);
      setLoadingRecientes(true);
      try {
        const [resTurnos, resRecientes] = await Promise.all([
          authedFetch(`${BASE_API_URL}/turno/mis-turnos`),
          authedFetch(`${BASE_API_URL}/turno/profesionales-recientes`),
        ]);
        if (!active) return;
        if (resTurnos.ok) setTurnos(await resTurnos.json());
        if (resRecientes.ok) setRecientes(await resRecientes.json());
      } finally {
        if (active) {
          setLoadingTurnos(false);
          setLoadingRecientes(false);
        }
      }
    }

    cargar();
    return () => {
      active = false;
    };
  }, [tick]);

  const [turnoACancelar, setTurnoACancelar] = useState<number | null>(null);
  const [actionError, setActionError] = useState("");

  const cancelarTurno = async (turnoId: number) => {
    setActionError("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estado: "CANCELADO" }),
      });
      if (res.ok) refresh();
      else setActionError("No se pudo cancelar el turno. Intentá de nuevo.");
    } catch {
      setActionError("Error de conexión.");
    }
  };

  const hoy = new Date().toISOString().split("T")[0];
  const proximosTurnos = turnos
    .filter((t) => (t.fecha >= hoy || t.estado === "REPROGRAMAR") && t.estado !== "CANCELADO")
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.bloqueHorario.localeCompare(b.bloqueHorario));

  return (
    <CommonLayout>
      <div className={styles.dashboard}>
        {/* ── COLUMNA IZQUIERDA ── */}
        <div className={styles.leftCol}>
          <button className={styles.btnTurno} onClick={() => navigate("/pedir-turno")}>
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
            Pedir turno
          </button>

          <section className={styles.card}>
            <h2 className={styles.sectionTitle}>Próximos turnos</h2>
            {actionError && <p className={styles.errorMsg}>{actionError}</p>}
            {loadingTurnos ? (
              <p className={styles.hint}>Cargando...</p>
            ) : proximosTurnos.length === 0 ? (
              <p className={styles.hint}>No tenés turnos próximos</p>
            ) : (
              <ul className={styles.turnosList}>
                {proximosTurnos.map((t) => (
                  <li key={t.id} className={styles.turnoItem}>
                    <Avatar src={t.profesionalFotoPerfil} nombre={t.profesionalNombre} apellido={t.profesionalApellido} size={40} />
                    <div style={{ flex: 1 }}>
                      <div className={styles.turnoNombre}>
                        {t.profesionalNombre} {t.profesionalApellido}
                      </div>
                      <div className={styles.turnoFecha}>{formatProfesionalInfoFromTurno(t)}</div>
                      <div className={styles.turnoFecha}>
                        {formatFecha(t.fecha)} · {t.bloqueHorario}hs
                        {t.servicioNombre && (
                          <>
                            {" "}
                            · {t.servicioNombre} · ${t.servicioPrecio}
                          </>
                        )}
                      </div>
                      <div className={`${styles.turnoEstado} ${styles[`estado${t.estado}`]}`}>
                        {labelEstado(t.estado)}
                      </div>
                    </div>
                    {t.estado === "OCUPADO_SIN_CONFIRMAR" && (
                      <button className={styles.btnConfirmar} onClick={() => navigate(`/pago/${t.id}`)}>
                        Pagar seña
                      </button>
                    )}
                    {t.estado === "REPROGRAMAR" && (
                      <button className={styles.btnReprogramar} onClick={() => navigate(`/reprogramar/${t.id}`)}>
                        Reprogramar
                      </button>
                    )}
                    <button className={styles.btnCancelar} onClick={() => setTurnoACancelar(t.id)}>
                      Cancelar
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        {/* ── COLUMNA DERECHA ── */}
        <div className={styles.rightCol}>
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

          <section className={styles.card}>
            <h2 className={styles.sectionTitle}>Profesionales recientes</h2>
            {loadingRecientes ? (
              <p className={styles.hint}>Cargando...</p>
            ) : recientes.length === 0 ? (
              <p className={styles.hint}>Todavía no tuviste turnos</p>
            ) : (
              <ul className={styles.recientesList}>
                {recientes.map((p) => (
                  <li key={p.id} className={styles.recienteItem} onClick={() => navigate(`/profesional/${p.id}`)}>
                    <Avatar src={p.fotoPerfil} nombre={p.nombre} apellido={p.apellido} size={28} />
                    <div>
                      <div className={styles.recienteNombre}>
                        {p.nombre} {p.apellido}
                      </div>
                      <div className={styles.recienteEmail}>{formatProfesionalInfo(p)}</div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
      {turnoACancelar !== null && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h3 className={styles.modalTitle}>¿Cancelar turno?</h3>
            <p className={styles.modalText}>Esta acción no se puede deshacer.</p>
            <div className={styles.modalActions}>
              <button
                className={styles.btnConfirmarCancelacion}
                onClick={() => {
                  cancelarTurno(turnoACancelar);
                  setTurnoACancelar(null);
                }}
              >
                Sí, cancelar
              </button>
              <button className={styles.btnModalVolver} onClick={() => setTurnoACancelar(null)}>
                No, volver
              </button>
            </div>
          </div>
        </div>
      )}
    </CommonLayout>
  );
};
