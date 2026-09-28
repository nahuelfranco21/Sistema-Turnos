import React, { useState } from "react";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./CambioPasswordLogueado.module.css";

export const CambioPasswordLogueado = () => {
  const [currentPassword, setCurrentPassword] = useState<string>("");
  const [newPassword, setNewPassword] = useState<string>("");
  const [repeatPassword, setRepeatPassword] = useState<string>("");
  const [success, setSuccess] = useState<boolean>(false);
  const [serverError, setServerError] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authedFetch = useAuthenticatedFetch();

  const [touched, setTouched] = useState({ current: false, newP: false, repeat: false });

  const [currentErrorMsg, setCurrentErrorMsg] = useState<string>("");
  const [newErrorMsg, setNewErrorMsg] = useState<string>("");
  const [repeatErrorMsg, setRepeatErrorMsg] = useState<string>("");

  const passwordPolicy = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

  const validateCurrent = (value: string) => {
    if (!value) return "La contraseña actual es obligatoria";
    return "";
  };

  const validateNew = (value: string) => {
    if (!value) return "La nueva contraseña es obligatoria";
    if (!passwordPolicy.test(value)) {
      return "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial";
    }
    return "";
  };

  const validateRepeat = (value: string, original: string) => {
    if (!value) return "Debes repetir la nueva contraseña";
    if (value !== original) return "Las contraseñas no coinciden";
    return "";
  };

  const handleSubmit = (e: React.FormEvent) => {
    const curErr = validateCurrent(currentPassword);
    const newErr = validateNew(newPassword);
    const repErr = validateRepeat(repeatPassword, newPassword);

    setCurrentErrorMsg(curErr);
    setNewErrorMsg(newErr);
    setRepeatErrorMsg(repErr);

    setTouched({ current: true, newP: true, repeat: true });

    if (curErr || newErr || repErr) {
      e.preventDefault();
      return;
    }

    e.preventDefault();

    setIsLoading(true);
    setSuccess(false);
    setServerError("");

    (async () => {
      try {
        const trimmedCurrent = currentPassword.trim();
        const response = await authedFetch(BASE_API_URL + "/users/password", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ currentPassword: trimmedCurrent, newPassword }),
        });

        const json = await response.json().catch(() => ({}));

        if (response.ok) {
          setSuccess(true);
          setServerError("");
          setCurrentPassword("");
          setNewPassword("");
          setRepeatPassword("");
          setTouched({ current: false, newP: false, repeat: false });
          setCurrentErrorMsg("");
          setNewErrorMsg("");
          setRepeatErrorMsg("");
          setIsLoading(false);
        } else {
          const err = (json && (json.error || json.message)) || `Error ${response.status}`;
          console.log("[CambioPassword] Server responded with status:", response.status);
          setServerError(err);
          setSuccess(false);
          setIsLoading(false);
        }
      } catch (err: unknown) {
        if (err instanceof Error) {
          setServerError(err.message);
        } else {
          setServerError("Ocurrió un error");
        }

        setSuccess(false);
        setIsLoading(false);
      }
    })();
  };

  const currentError = touched.current && !!currentErrorMsg;
  const newError = touched.newP && !!newErrorMsg;
  const repeatError = touched.repeat && !!repeatErrorMsg;

  return (
    <CommonLayout>
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Cambiar Contraseña</h1>
          <form onSubmit={handleSubmit}>
            {success && <div className={styles.success}>Contraseña actualizada con éxito</div>}
            {serverError && <div className={styles.error}>{serverError}</div>}
            <div className={styles.field}>
              <label className="label">Contraseña actual</label>
              <input
                type="password"
                className={`input ${currentError ? styles.inputError : ""}`}
                value={currentPassword}
                onChange={(e) => {
                  setCurrentPassword(e.target.value);
                  if (touched.current) setCurrentErrorMsg(validateCurrent(e.target.value));
                }}
                onBlur={() => {
                  setTouched((s) => ({ ...s, current: true }));
                  setCurrentErrorMsg(validateCurrent(currentPassword));
                }}
                required
              />
              {currentError && <div className={styles.error}>{currentErrorMsg}</div>}
            </div>

            <div className={styles.field}>
              <label className="label">Nueva contraseña</label>
              <input
                type="password"
                className={`input ${newError ? styles.inputError : ""}`}
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (touched.newP) setNewErrorMsg(validateNew(e.target.value));
                  if (touched.repeat) setRepeatErrorMsg(validateRepeat(repeatPassword, e.target.value));
                }}
                onBlur={() => {
                  setTouched((s) => ({ ...s, newP: true }));
                  setNewErrorMsg(validateNew(newPassword));
                }}
                required
              />
              {newError && <div className={styles.error}>{newErrorMsg}</div>}
            </div>

            <div className={styles.field}>
              <label className="label">Repetir nueva contraseña</label>
              <input
                type="password"
                className={`input ${repeatError ? styles.inputError : ""}`}
                value={repeatPassword}
                onChange={(e) => {
                  setRepeatPassword(e.target.value);
                  if (touched.repeat) setRepeatErrorMsg(validateRepeat(e.target.value, newPassword));
                }}
                onBlur={() => {
                  setTouched((s) => ({ ...s, repeat: true }));
                  setRepeatErrorMsg(validateRepeat(repeatPassword, newPassword));
                }}
                required
              />
              {repeatError && <div className={styles.error}>{repeatErrorMsg}</div>}
            </div>

            <div className={styles.actions}>
              <button type="submit" className="button" disabled={isLoading}>
                {isLoading ? "Cargando..." : "Actualizar Contraseña"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </CommonLayout>
  );
};

export default CambioPasswordLogueado;
