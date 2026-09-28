import { useRef, useState } from "react";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";
import { Avatar } from "../../components/Avatar/Avatar";
import styles from "./Perfil.module.css";

type Props = {
  fotoPerfil: string | null;
  nombre: string;
  apellido: string;
  onFotoUpdate: (base64: string) => void;
};

export const FotoPerfil = ({ fotoPerfil, nombre, apellido, onFotoUpdate }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [fotoError, setFotoError] = useState("");
  const [fotoExito, setFotoExito] = useState(false);
  const [verFoto, setVerFoto] = useState(false);

  const handleFotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setFotoError("El archivo debe ser una imagen");
      return;
    }

    if (file.size > 1048576) {
      setFotoError("La imagen no puede pesar más de 1MB");
      return;
    }

    const reader = new FileReader();
    reader.onload = async () => {
      const base64 = reader.result as string;

      try {
        const res = await authedFetch(`${BASE_API_URL}/users/foto`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ fotoPerfil: base64 }),
        });

        if (!res.ok) throw new Error("Error al actualizar la foto");

        onFotoUpdate(base64);
        setFotoExito(true);
        setFotoError("");
        setTimeout(() => setFotoExito(false), 3000);
      } catch {
        setFotoError("No se pudo actualizar la foto");
      }
    };
    reader.readAsDataURL(file);
  };

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        style={{ display: "none" }}
        onChange={handleFotoChange}
      />

      <div className={styles.avatarContainer}>
        <div
          onClick={() => {
            if (fotoPerfil) setVerFoto(true);
          }}
          className={styles.avatar}
          title={fotoPerfil ? "Ver foto de perfil" : undefined}
        >
          <Avatar
            src={fotoPerfil}
            nombre={nombre}
            apellido={apellido}
            size={56}
          />
        </div>
        <span
          onClick={() => fileInputRef.current?.click()}
          className={styles.editLink}
        >
          Editar
        </span>
        {fotoError && <p className={styles.fieldError}>{fotoError}</p>}
        {fotoExito && <p className={styles.fieldSuccess}>Foto actualizada</p>}
      </div>

      {verFoto && fotoPerfil && (
        <div
          onClick={() => setVerFoto(false)}
          className={styles.overlay}
        >
          <img
            src={fotoPerfil}
            alt="Foto de perfil"
            className={styles.overlayImg}
          />
          <p className={styles.overlayHint}>
            Click para cerrar
          </p>
        </div>
      )}
    </>
  );
};
