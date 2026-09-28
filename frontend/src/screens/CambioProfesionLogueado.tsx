import React, { useState } from "react";
import { Redirect, useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch, useToken } from "@/services/TokenContext";

import { SECTORES, COLORS } from "../constants";

import styles from "./CambioPasswordLogueado.module.css";

export const CambioProfesionLogueado = () => {
  const [profesion, setProfesion] = useState<string>("");
  const [sector, setSector] = useState<string>("");
  const [ubicacion, setUbicacion] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const authedFetch = useAuthenticatedFetch();

  const [serverError, setServerError] = useState<string>("");
  const [, setLocation] = useLocation();

  const [tokenState] = useToken();
  const roles = tokenState.state === "LOGGED_IN" ? tokenState.tokens.roles : [];
  const isCliente = roles.includes("CLIENTE") && !roles.includes("PROFESIONAL");

  if (isCliente) {
    return <Redirect href="/perfil" />;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!profesion || !profesion.trim()) {
      setServerError("La profesión es obligatoria");
      return;
    }

    if (!ubicacion || !ubicacion.trim()) {
      setServerError("La ubicación es obligatoria");
      return;
    }

    setIsLoading(true);
    setServerError("");
    setSuccess(false);

    (async () => {
      try {
        const response = await authedFetch(BASE_API_URL + "/users/profesion", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ profesion: profesion.trim(), sector: sector || null, ubicacion: ubicacion.trim() }),
        });

        const json = await response.json().catch(() => ({}));

        if (response.ok) {
          setSuccess(true);
          setProfesion("");
          setSector("");
          setUbicacion("");
          setLocation("/perfil");
        } else {
          const err = (json && (json.error || json.message)) || `Error ${response.status}`;
          setServerError(err);
        }
      } catch (err: unknown) {
        if (err instanceof Error) setServerError(err.message);
        else setServerError("Ocurrió un error");
      } finally {
        setIsLoading(false);
      }
    })();
  };

  return (
    <CommonLayout>
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Cambiar Profesión</h1>
          <form onSubmit={handleSubmit}>
            {success && <div className={styles.success}>Profesión actualizada</div>}
            {serverError && <div className={styles.error}>{serverError}</div>}

            <div className={styles.field} style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <label className="label" style={{ marginRight: 8 }}>
                Nueva profesión
              </label>
              <input
                type="text"
                className="input"
                value={profesion}
                onChange={(e) => setProfesion(e.target.value)}
                placeholder="Ingresá la nueva profesión"
                style={{ flex: 1 }}
              />

              <div style={{ display: "flex", flexDirection: "column", minWidth: 220 }}>
                <label style={{ fontSize: 14, fontWeight: 500 }}>Sector</label>
                <select
                  value={sector}
                  onChange={(e) => setSector(e.target.value)}
                  style={{
                    display: "block",
                    marginTop: 4,
                    padding: "8px 12px",
                    borderRadius: 6,
                    border: `1px solid ${COLORS.borderGray}`,
                    fontSize: 14,
                    width: "100%",
                  }}
                  className="input"
                >
                  <option value="">Seleccioná un sector</option>
                  {SECTORES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className={styles.field} style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 12 }}>
              <label className="label" style={{ marginRight: 8 }}>
                Ubicación
              </label>
              <input
                type="text"
                className="input"
                value={ubicacion}
                onChange={(e) => setUbicacion(e.target.value)}
                placeholder="Ingresá la ubicación"
                style={{ flex: 1 }}
              />
            </div>

            <div className={styles.actions}>
              <button type="submit" className="button" disabled={isLoading}>
                {isLoading ? "Cargando..." : "Actualizar Profesión"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </CommonLayout>
  );
};

export default CambioProfesionLogueado;
