import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch, useToken } from "@/services/TokenContext";

import styles from "./CalendarScreen.module.css";

type TurnoDTO = {
  id: number;
  profesionalNombre: string;
  profesionalApellido: string;
  profesionalProfesion: string | null;
  profesionalSector: string | null;
  profesionalUbicacion: string | null;
  clienteNombre: string | null;
  clienteApellido: string | null;
  nombreCliente: string | null;
  clienteFotoPerfil: string | null;
  profesionalFotoPerfil: string | null;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};

const MESES = [
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre",
];
const DIAS_SEMANA = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];

export const CalendarScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [tokenState] = useToken();
  const [, navigate] = useLocation();

  const roles = tokenState.state === "LOGGED_IN" ? tokenState.tokens.roles : [];
  const isProfesional = roles.includes("PROFESIONAL");

  const hoy = new Date();
  const [anio, setAnio] = useState(hoy.getFullYear());
  const [mes, setMes] = useState(hoy.getMonth()); // 0-based
  const [turnos, setTurnos] = useState<TurnoDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    (async () => {
      try {
        const endpoint = isProfesional ? "/agenda/mi-agenda" : "/turno/mis-turnos";
        const res = await authedFetch(`${BASE_API_URL}${endpoint}`);
        if (!active || !res.ok) return;
        const data = await res.json();
        const lista: TurnoDTO[] = isProfesional ? data.turnos : data;
        setTurnos(lista);
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const primerDia = new Date(anio, mes, 1).getDay(); // 0=Dom
  const diasEnMes = new Date(anio, mes + 1, 0).getDate();

  const turnosPorDia: Record<number, TurnoDTO[]> = {};
  turnos.forEach((t) => {
    const [y, m, d] = t.fecha.split("-").map(Number);
    if (y === anio && m - 1 === mes) {
      turnosPorDia[d] = [...(turnosPorDia[d] ?? []), t];
    }
  });

  const celdas: (number | null)[] = [
    ...Array(primerDia).fill(null),
    ...Array.from({ length: diasEnMes }, (_, i) => i + 1),
  ];
  while (celdas.length % 7 !== 0) celdas.push(null);

  const anterior = () => {
    if (mes === 0) {
      setAnio((a) => a - 1);
      setMes(11);
    } else setMes((m) => m - 1);
  };
  const siguiente = () => {
    if (mes === 11) {
      setAnio((a) => a + 1);
      setMes(0);
    } else setMes((m) => m + 1);
  };

  const todayStr = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(hoy.getDate()).padStart(2, "0")}`;

  return (
    <CommonLayout>
      <div className={styles.wrapper}>
        <div className={styles.header}>
          <button className={styles.navBtn} onClick={anterior}>
            ‹
          </button>
          <h2 className={styles.titulo}>
            {MESES[mes]} {anio}
          </h2>
          <button className={styles.navBtn} onClick={siguiente}>
            ›
          </button>
          <button className={styles.backBtn} onClick={() => navigate(isProfesional ? "/agenda" : "/profesionales")}>
            ← Volver al dashboard
          </button>
        </div>

        {loading ? (
          <p className={styles.hint}>Cargando turnos...</p>
        ) : (
          <div className={styles.grid}>
            {DIAS_SEMANA.map((d) => (
              <div key={d} className={styles.diaHeader}>
                {d}
              </div>
            ))}
            {celdas.map((dia, i) => {
              if (dia === null) return <div key={i} className={styles.celdaVacia} />;
              const fechaStr = `${anio}-${String(mes + 1).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
              const esHoy = fechaStr === todayStr;
              const turnosDia = turnosPorDia[dia] ?? [];
              return (
                <div key={i} className={`${styles.celda} ${esHoy ? styles.celdaHoy : ""}`}>
                  <span className={styles.numeroDia}>{dia}</span>
                  {turnosDia.map((t) => (
                    <div key={t.id} className={`${styles.turnoChip} ${styles[`estado${t.estado}`]}`}>
                      {t.bloqueHorario}hs —{" "}
                      {isProfesional
                        ? `${t.clienteNombre ?? t.nombreCliente ?? "?"}${t.clienteApellido ? ` ${t.clienteApellido}` : ""}`
                        : `${t.profesionalNombre} ${t.profesionalApellido}`}
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </CommonLayout>
  );
};
