import { useState } from "react";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";
import styles from "./Perfil.module.css";

type Props = {
  descripcion: string | null;
  onDescripcionUpdate: (descripcion: string) => void;
};

export const DescripcionEditor = ({ descripcion, onDescripcionUpdate }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const [editandoDesc, setEditandoDesc] = useState(false);
  const [descEdit, setDescEdit] = useState("");
  const [descError, setDescError] = useState("");
  const [descExito, setDescExito] = useState(false);
  const [guardandoDesc, setGuardandoDesc] = useState(false);

  const handleGuardarDescripcion = async () => {
    setGuardandoDesc(true);
    setDescError("");
    setDescExito(false);
    try {
      const res = await authedFetch(`${BASE_API_URL}/users/descripcion`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ descripcion: descEdit.trim() }),
      });
      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error(json.error ?? "Error al actualizar la descripción");
      }
      onDescripcionUpdate(descEdit.trim());
      setEditandoDesc(false);
      setDescExito(true);
      setTimeout(() => setDescExito(false), 3000);
    } catch (e: unknown) {
      setDescError(e instanceof Error ? e.message : "Error");
    } finally {
      setGuardandoDesc(false);
    }
  };

  return (
    <div className={styles.editorSection}>
      <div className={styles.editorHeader}>
        <span className={styles.editorLabel}>Descripción</span>
        {!editandoDesc && (
          <button
            onClick={() => {
              setDescEdit(descripcion ?? "");
              setEditandoDesc(true);
            }}
            className={styles.editorButton}
          >
            {descripcion ? "Editar" : "Agregar"}
          </button>
        )}
      </div>
      {editandoDesc ? (
        <div className={styles.editorField}>
          <textarea
            value={descEdit}
            onChange={(e) => setDescEdit(e.target.value)}
            rows={4}
            className={styles.editorTextarea}
            placeholder="Contanos sobre tu experiencia, especialidad, etc."
          />
          <div className={styles.btnGroup}>
            <button
              onClick={handleGuardarDescripcion}
              disabled={guardandoDesc}
              className={styles.btnSave}
            >
              {guardandoDesc ? "Guardando..." : "Guardar"}
            </button>
            <button
              onClick={() => setEditandoDesc(false)}
              className={styles.btnCancel}
            >
              Cancelar
            </button>
          </div>
          {descError && <p className={styles.fieldError}>{descError}</p>}
        </div>
      ) : (
        <span className={styles.editorValue}>
          {descripcion || "—"}
        </span>
      )}
      {descExito && <p className={styles.fieldSuccess}>Descripción actualizada</p>}
    </div>
  );
};
