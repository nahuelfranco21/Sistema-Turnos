import { useState } from "react";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";
import styles from "./Perfil.module.css";

type Props = {
  ubicacion: string | null;
  profesion: string | null;
  sector: string | null;
  onUbicacionUpdate: (ubicacion: string) => void;
};

export const UbicacionEditor = ({ ubicacion, profesion, sector, onUbicacionUpdate }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const [editandoUbicacion, setEditandoUbicacion] = useState(false);
  const [ubicacionEdit, setUbicacionEdit] = useState("");
  const [ubicacionError, setUbicacionError] = useState("");
  const [ubicacionExito, setUbicacionExito] = useState(false);
  const [guardandoUbicacion, setGuardandoUbicacion] = useState(false);

  const handleGuardarUbicacion = async () => {
    setGuardandoUbicacion(true);
    setUbicacionError("");
    setUbicacionExito(false);
    try {
      const res = await authedFetch(`${BASE_API_URL}/users/profesion`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          profesion,
          sector,
          ubicacion: ubicacionEdit.trim(),
        }),
      });
      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error(json.error ?? "Error al actualizar la ubicación");
      }
      onUbicacionUpdate(ubicacionEdit.trim());
      setEditandoUbicacion(false);
      setUbicacionExito(true);
      setTimeout(() => setUbicacionExito(false), 3000);
    } catch (e: unknown) {
      setUbicacionError(e instanceof Error ? e.message : "Error");
    } finally {
      setGuardandoUbicacion(false);
    }
  };

  return (
    <div className={styles.editorSection}>
      <div className={styles.editorHeader}>
        <span className={styles.editorLabel}>Ubicación</span>
        {!editandoUbicacion && (
          <button
            onClick={() => {
              setUbicacionEdit(ubicacion ?? "");
              setEditandoUbicacion(true);
            }}
            className={styles.editorButton}
          >
            {ubicacion ? "Editar" : "Agregar"}
          </button>
        )}
      </div>
      {editandoUbicacion ? (
        <div className={styles.editorField}>
          <input
            type="text"
            value={ubicacionEdit}
            onChange={(e) => setUbicacionEdit(e.target.value)}
            className={styles.editorInput}
            placeholder="Ingresá tu ubicación"
          />
          <div className={styles.btnGroup}>
            <button
              onClick={handleGuardarUbicacion}
              disabled={guardandoUbicacion}
              className={styles.btnSave}
            >
              {guardandoUbicacion ? "Guardando..." : "Guardar"}
            </button>
            <button
              onClick={() => setEditandoUbicacion(false)}
              className={styles.btnCancel}
            >
              Cancelar
            </button>
          </div>
          {ubicacionError && <p className={styles.fieldError}>{ubicacionError}</p>}
        </div>
      ) : (
        <span className={styles.editorValue}>{ubicacion ?? "—"}</span>
      )}
      {ubicacionExito && <p className={styles.fieldSuccess}>Ubicación actualizada</p>}
    </div>
  );
};
