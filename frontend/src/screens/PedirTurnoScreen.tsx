import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./PedirTurnoScreen.module.css";

import { SECTORES } from "../constants";
import { useDebounce } from "../hooks/useDebounce";
import { Avatar } from "../components/Avatar/Avatar";

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

export const PedirTurnoScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();

  const [resultados, setResultados] = useState<ProfesionalDTO[]>([]);
  const [loading, setLoading] = useState(true);

  const [busqueda, setBusqueda] = useState("");
  const [selectedSector, setSelectedSector] = useState<string>("");
  const [profesionQuery, setProfesionQuery] = useState<string>("");
  const [servicioQuery, setServicioQuery] = useState<string>("");

  const fetchResultados = async (filters: { q: string; sector: string; profesion: string; servicio: string }) => {
    setLoading(true);
    try {
      const qParam = encodeURIComponent(filters.q);
      const sectorParam = filters.sector ? `&sector=${encodeURIComponent(filters.sector)}` : "";
      const profesionParam = filters.profesion ? `&profesion=${encodeURIComponent(filters.profesion)}` : "";
      const servicioParam = filters.servicio ? `&servicio=${encodeURIComponent(filters.servicio)}` : "";
      const url = `${BASE_API_URL}/users?role=PROFESIONAL&q=${qParam}${sectorParam}${profesionParam}${servicioParam}`;
      const res = await authedFetch(url);
      if (res.ok) setResultados(await res.json());
      else setResultados([]);
    } finally {
      setLoading(false);
    }
  };

  const debouncedBusqueda = useDebounce(busqueda, 300);
  const debouncedSector = useDebounce(selectedSector, 300);
  const debouncedProfesion = useDebounce(profesionQuery, 300);
  const debouncedServicio = useDebounce(servicioQuery, 300);

  useEffect(() => {
    fetchResultados({ q: "", sector: "", profesion: "", servicio: "" });
  }, []);

  useEffect(() => {
    fetchResultados({
      q: debouncedBusqueda,
      sector: debouncedSector,
      profesion: debouncedProfesion,
      servicio: debouncedServicio,
    });
  }, [debouncedBusqueda, debouncedSector, debouncedProfesion, debouncedServicio]);

  return (
    <CommonLayout>
      <div className={styles.page}>
        <div className={styles.topBar}>
          <button className={styles.btnVolver} onClick={() => navigate("/profesionales")}>
            ← Volver
          </button>
          <span className={styles.totalCount}>
            {loading ? "Cargando..." : `${resultados.length} profesional${resultados.length !== 1 ? "es" : ""}`}
          </span>
        </div>

        <div className={styles.filterBar}>
          <div className={styles.filterGroup}>
            <label className={styles.filterLabel}>Nombre</label>
            <input
              className={styles.filterInput}
              type="text"
              placeholder="Buscar..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </div>

          <div className={styles.filterGroup}>
            <label className={styles.filterLabel}>Sector</label>
            <select
              value={selectedSector}
              onChange={(e) => setSelectedSector(e.target.value)}
              className={styles.filterInput}
            >
              <option value="">Todos</option>
              {SECTORES.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>

          <div className={styles.filterGroup}>
            <label className={styles.filterLabel}>Profesión</label>
            <input
              className={styles.filterInput}
              type="text"
              placeholder="Ej: psicólogo..."
              value={profesionQuery}
              onChange={(e) => setProfesionQuery(e.target.value)}
            />
          </div>

          <div className={styles.filterGroup}>
            <label className={styles.filterLabel}>Servicio</label>
            <input
              className={styles.filterInput}
              type="text"
              placeholder="Ej: consulta..."
              value={servicioQuery}
              onChange={(e) => setServicioQuery(e.target.value)}
            />
          </div>
        </div>

        {loading ? (
          <p className={styles.loadingHint}>Cargando profesionales...</p>
        ) : resultados.length === 0 ? (
          <p className={styles.loadingHint}>No se encontraron profesionales con esos filtros.</p>
        ) : (
          <div className={styles.grid}>
            {resultados.map((p) => (
              <div key={p.id} className={styles.card} onClick={() => navigate(`/profesional/${p.id}`)}>
                <Avatar src={p.fotoPerfil} nombre={p.nombre} apellido={p.apellido} size={48} />
                <div className={styles.cardName}>
                  {p.nombre} {p.apellido}
                </div>
                <div className={styles.cardInfo}>{[p.sector, p.profesion].filter(Boolean).join(" · ")}</div>
                {p.ubicacion && (
                  <div className={styles.cardUbicacion}>
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                    {p.ubicacion}
                  </div>
                )}
                {p.servicios && p.servicios.length > 0 && (
                  <div className={styles.cardServicios}>
                    {p.servicios.slice(0, 2).map((s) => (
                      <span key={s.id} className={styles.servicioChip}>
                        {s.nombre} — ${s.precio}
                      </span>
                    ))}
                    {p.servicios.length > 2 && (
                      <span className={styles.servicioChip}>+{p.servicios.length - 2} más</span>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </CommonLayout>
  );
};
