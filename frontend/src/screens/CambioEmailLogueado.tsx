import React, { useState } from "react";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./CambioPasswordLogueado.module.css";

export const CambioEmailLogueado = () => {
  const [newEmail, setNewEmail] = useState<string>("");
  const [repeatEmail, setRepeatEmail] = useState<string>("");
  const [success, setSuccess] = useState<boolean>(false);
  const [serverError, setServerError] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authedFetch = useAuthenticatedFetch();

  const [touched, setTouched] = useState({ newE: false, repeat: false });
  const [newErrorMsg, setNewErrorMsg] = useState<string>("");
  const [repeatErrorMsg, setRepeatErrorMsg] = useState<string>("");

  const validateEmail = (value: string) => {
    if (!value) return "El email es obligatorio";
    const re = /^\S+@\S+\.\S+$/;
    if (!re.test(value)) return "Ingresá un email válido";
    return "";
  };

  const validateRepeat = (value: string, original: string) => {
    if (!value) return "Debes repetir el email";
    if (value !== original) return "Los emails no coinciden";
    return "";
  };

  const handleSubmit = (e: React.FormEvent) => {
    const newErr = validateEmail(newEmail);
    const repErr = validateRepeat(repeatEmail, newEmail);

    setNewErrorMsg(newErr);
    setRepeatErrorMsg(repErr);
    setTouched({ newE: true, repeat: true });

    if (newErr || repErr) {
      e.preventDefault();
      return;
    }

    e.preventDefault();

    setIsLoading(true);
    setSuccess(false);
    setServerError("");

    (async () => {
      try {
        const response = await authedFetch(BASE_API_URL + "/users/email", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: newEmail }),
        });

        const json = await response.json().catch(() => ({}));

        if (response.ok) {
          setSuccess(true);
          setServerError("");
          setNewEmail("");
          setRepeatEmail("");
          setTouched({ newE: false, repeat: false });
          setNewErrorMsg("");
          setRepeatErrorMsg("");
          setIsLoading(false);
        } else {
          const err = (json && (json.error || json.message)) || `Error ${response.status}`;
          setServerError(err);
          setSuccess(false);
          setIsLoading(false);
        }
      } catch (err: unknown) {
        if (err instanceof Error) setServerError(err.message);
        else setServerError("Ocurrió un error");
        setSuccess(false);
        setIsLoading(false);
      }
    })();
  };

  const newError = touched.newE && !!newErrorMsg;
  const repeatError = touched.repeat && !!repeatErrorMsg;

  return (
    <CommonLayout>
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Cambiar Email</h1>
          <form onSubmit={handleSubmit}>
            {success && <div className={styles.success}>Email actualizado con éxito</div>}
            {serverError && <div className={styles.error}>{serverError}</div>}

            <div className={styles.field}>
              <label className="label">Nuevo email</label>
              <input
                type="email"
                className={`input ${newError ? styles.inputError : ""}`}
                value={newEmail}
                onChange={(e) => {
                  setNewEmail(e.target.value);
                  if (touched.newE) setNewErrorMsg(validateEmail(e.target.value));
                  if (touched.repeat) setRepeatErrorMsg(validateRepeat(repeatEmail, e.target.value));
                }}
                onBlur={() => {
                  setTouched((s) => ({ ...s, newE: true }));
                  setNewErrorMsg(validateEmail(newEmail));
                }}
                required
              />
              {newError && <div className={styles.error}>{newErrorMsg}</div>}
            </div>

            <div className={styles.field}>
              <label className="label">Repetir nuevo email</label>
              <input
                type="email"
                className={`input ${repeatError ? styles.inputError : ""}`}
                value={repeatEmail}
                onChange={(e) => {
                  setRepeatEmail(e.target.value);
                  if (touched.repeat) setRepeatErrorMsg(validateRepeat(e.target.value, newEmail));
                }}
                onBlur={() => {
                  setTouched((s) => ({ ...s, repeat: true }));
                  setRepeatErrorMsg(validateRepeat(repeatEmail, newEmail));
                }}
                required
              />
              {repeatError && <div className={styles.error}>{repeatErrorMsg}</div>}
            </div>

            <div className={styles.actions}>
              <button type="submit" className="button" disabled={isLoading}>
                {isLoading ? "Cargando..." : "Actualizar Email"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </CommonLayout>
  );
};

export default CambioEmailLogueado;
