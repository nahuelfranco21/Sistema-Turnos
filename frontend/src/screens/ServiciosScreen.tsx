import { useEffect, useState } from "react";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch, useToken } from "@/services/TokenContext";

import styles from "./Servicios.module.css";

type Servicio = {
  id: number;
  nombre: string;
  precio: number;
  activo: boolean;
};

export const ServiciosScreen = () => {
  const authedFetch = useAuthenticatedFetch();
  const [tokenState] = useToken();

  const accessToken = tokenState.state === "LOGGED_IN" ? tokenState.tokens.accessToken : null;

  const [agendaId, setAgendaId] = useState<number | null>(null);
  const [servicios, setServicios] = useState<Servicio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [nuevoNombre, setNuevoNombre] = useState("");
  const [nuevoPrecio, setNuevoPrecio] = useState("");
  const [agregando, setAgregando] = useState(false);
  const [addError, setAddError] = useState("");

  useEffect(() => {
    if (accessToken === null) return;
    let active = true;
    setLoading(true);
    setError("");
    (async () => {
      try {
        const resAgenda = await authedFetch(`${BASE_API_URL}/agenda/mi-agenda`);
        if (!resAgenda.ok) throw new Error(`Error ${resAgenda.status} al cargar la agenda`);
        const agendaData = await resAgenda.json();
        const id: number = agendaData.agenda.id;
        if (!active) return;
        setAgendaId(id);

        const resServicios = await authedFetch(`${BASE_API_URL}/servicio/agenda/${id}`);
        if (!resServicios.ok) throw new Error(`Error ${resServicios.status} al cargar servicios`);
        if (active) setServicios(await resServicios.json());
      } catch (e: unknown) {
        if (active) setError(e instanceof Error ? e.message : "Error al cargar");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [accessToken]);

  const agregarServicio = async () => {
    if (!nuevoNombre.trim() || !nuevoPrecio || agendaId === null) return;
    setAgregando(true);
    setAddError("");
    try {
      const res = await authedFetch(`${BASE_API_URL}/servicio`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          agendaId,
          nombre: nuevoNombre.trim(),
          precio: parseFloat(nuevoPrecio),
        }),
      });
      if (!res.ok) throw new Error("No se pudo crear el servicio");
      const nuevo: Servicio = await res.json();
      setServicios((prev) => [...prev, nuevo]);
      setNuevoNombre("");
      setNuevoPrecio("");
    } catch (e: unknown) {
      setAddError(e instanceof Error ? e.message : "Error al agregar");
    } finally {
      setAgregando(false);
    }
  };

  const toggleActivo = async (id: number) => {
    try {
      const res = await authedFetch(`${BASE_API_URL}/servicio/${id}/toggle-activo`, {
        method: "PATCH",
      });
      if (res.ok) {
        const updated: Servicio = await res.json();
        setServicios((prev) => prev.map((s) => (s.id === id ? updated : s)));
      }
    } catch {
      /* silencioso */
    }
  };

  const eliminarServicio = async (id: number) => {
    try {
      await authedFetch(`${BASE_API_URL}/servicio/${id}`, {
        method: "DELETE",
      });
      setServicios((prev) => prev.filter((s) => s.id !== id));
    } catch {
      /* silencioso */
    }
  };

  return (
    <CommonLayout>
      <div className={styles.container}>
        <h1 className={styles.title}>Mis Servicios</h1>

        {loading && <p>Cargando...</p>}
        {error && <p>{error}</p>}

        {!loading && !error && (
          <>
            <div className={styles.form}>
              <input
                type="text"
                placeholder="Nombre del servicio"
                value={nuevoNombre}
                onChange={(e) => setNuevoNombre(e.target.value)}
              />

              <input
                type="number"
                placeholder="Precio"
                value={nuevoPrecio}
                onChange={(e) => setNuevoPrecio(e.target.value)}
              />

              {addError && <p>{addError}</p>}

              <button onClick={agregarServicio} disabled={agregando || !nuevoNombre.trim() || !nuevoPrecio}>
                {agregando ? "Agregando..." : "Agregar servicio"}
              </button>
            </div>

            <div className={styles.servicesGrid}>
              {servicios.map((servicio) => (
                <div key={servicio.id} className={styles.card}>
                  <h2>{servicio.nombre}</h2>

                  <div className={styles.infoRow}>
                    <span className={styles.label}>Precio</span>
                    <span className={styles.value}>${servicio.precio}</span>
                  </div>

                  <div className={`${styles.badge} ${servicio.activo ? styles.badgeActive : styles.badgeInactive}`}>
                    {servicio.activo ? "Activo" : "Inactivo"}
                  </div>

                  <div className={styles.actions}>
                    <button
                      className={`${styles.actionBtn} ${servicio.activo ? styles.btnDeactivate : styles.btnActivate}`}
                      onClick={() => toggleActivo(servicio.id)}
                    >
                      {servicio.activo ? "Desactivar" : "Activar"}
                    </button>

                    <button className={styles.deleteButton} onClick={() => eliminarServicio(servicio.id)}>
                      Eliminar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </CommonLayout>
  );
};
