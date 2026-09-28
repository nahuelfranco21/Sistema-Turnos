import { useEffect, useState } from "react";
import { useLocation } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import { PAGO } from "../constants/pago";
import { formatFecha } from "../utils/formatters";
import styles from "./PagoMockScreen.module.css";

type TurnoDetalle = {
  id: number;
  profesionalNombre: string;
  profesionalApellido: string;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};

type Props = { turnoId: number };

export const PagoMockScreen = ({ turnoId }: Props) => {
  const authedFetch = useAuthenticatedFetch();
  const [, navigate] = useLocation();

  const [turno, setTurno] = useState<TurnoDetalle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [pagando, setPagando] = useState(false);
  const [pagoOk, setPagoOk] = useState(false);
  const [cardNumber, setCardNumber] = useState("");
  const [cardExpiry, setCardExpiry] = useState("");
  const [cardCvv, setCardCvv] = useState("");
  const [cardHolder, setCardHolder] = useState("");
  const [cardNumberError, setCardNumberError] = useState("");
  const [cardExpiryError, setCardExpiryError] = useState("");
  const [cardCvvError, setCardCvvError] = useState("");
  const [cardHolderError, setCardHolderError] = useState("");

  const validarCardNumber = (valor: string) => {
    if (!valor) return "El número de tarjeta es obligatorio";
    if (!new RegExp(`^\\d{${PAGO.CARD_NUMBER_LENGTH}}$`).test(valor)) return `El número de tarjeta debe tener exactamente ${PAGO.CARD_NUMBER_LENGTH} dígitos`;
    return "";
  };

  const validarCardExpiry = (valor: string) => {
    if (!valor) return "El vencimiento es obligatorio";
    const match = /^(\d{2})\/(\d{2})$/.exec(valor);
    if (!match) return "Formato inválido, usar MM/AA";
    const mes = Number(match[1]);
    const anio = Number(match[2]);
    if (mes < 1 || mes > 12) return "El mes debe estar entre 01 y 12";
    const ahora = new Date();
    const anioActual = ahora.getFullYear() % 100;
    const mesActual = ahora.getMonth() + 1;
    if (anio < anioActual || (anio === anioActual && mes < mesActual)) {
      return "La tarjeta está vencida";
    }
    return "";
  };

  const validarCardCvv = (valor: string) => {
    if (!valor) return "El CVV es obligatorio";
    if (!new RegExp(`^\\d{${PAGO.CVV_MIN_LENGTH},${PAGO.CVV_MAX_LENGTH}}$`).test(valor)) return `El CVV debe tener ${PAGO.CVV_MIN_LENGTH} o ${PAGO.CVV_MAX_LENGTH} dígitos`;
    return "";
  };

  const validarCardHolder = (valor: string) => {
    const limpio = valor.trim();
    if (!limpio) return "El titular es obligatorio";
    if (limpio.length < PAGO.HOLDER_MIN_LENGTH) return `El titular debe tener al menos ${PAGO.HOLDER_MIN_LENGTH} caracteres`;
    if (!/^[a-zA-ZÀ-ÿ\s'.-]+$/.test(limpio)) return "El titular solo puede contener letras y espacios";
    return "";
  };

  const handleCardNumberChange = (valor: string) => {
    const digits = valor.replace(/\D/g, "").slice(0, PAGO.CARD_NUMBER_LENGTH);
    setCardNumber(digits);
    setCardNumberError(validarCardNumber(digits));
  };

  const handleCardExpiryChange = (valor: string) => {
    setCardExpiry(valor);
    setCardExpiryError(validarCardExpiry(valor));
  };

  const handleCardCvvChange = (valor: string) => {
    setCardCvv(valor);
    setCardCvvError(validarCardCvv(valor));
  };

  const handleCardHolderChange = (valor: string) => {
    setCardHolder(valor);
    setCardHolderError(validarCardHolder(valor));
  };

  const formularioValido =
    !validarCardNumber(cardNumber) &&
    !validarCardExpiry(cardExpiry) &&
    !validarCardCvv(cardCvv) &&
    !validarCardHolder(cardHolder);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`);
        if (!res.ok) throw new Error("No se pudo cargar el turno");
        if (active) setTurno(await res.json());
      } catch (e: unknown) {
        if (active) setError(e instanceof Error ? e.message : "Error");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [turnoId]);

  const confirmarPago = async () => {
    if (!turno) return;

    const errorNumber = validarCardNumber(cardNumber);
    const errorExpiry = validarCardExpiry(cardExpiry);
    const errorCvv = validarCardCvv(cardCvv);
    const errorHolder = validarCardHolder(cardHolder);
    setCardNumberError(errorNumber);
    setCardExpiryError(errorExpiry);
    setCardCvvError(errorCvv);
    setCardHolderError(errorHolder);
    if (errorNumber || errorExpiry || errorCvv || errorHolder) return;

    setPagando(true);
    try {
      const res = await authedFetch(`${BASE_API_URL}/turno/${turnoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estado: "CONFIRMADO" }),
      });
      if (!res.ok) throw new Error("Error al confirmar el pago");
      setPagoOk(true);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Error al procesar el pago");
    } finally {
      setPagando(false);
    }
  };

  const cancelarPago = async () => {
    if (!turno) return;
    try {
      await authedFetch(`${BASE_API_URL}/turno/${turnoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estado: "CANCELADO" }),
      });
    } catch {
      /* silencioso */
    }
    navigate("/profesionales");
  };

  if (loading) {
    return (
      <CommonLayout>
        <div className={styles.page}>
          <p className={styles.hint}>Cargando...</p>
        </div>
      </CommonLayout>
    );
  }

  if (error && !pagoOk) {
    return (
      <CommonLayout>
        <div className={styles.page}>
          <p className={styles.error}>{error}</p>
        </div>
      </CommonLayout>
    );
  }

  if (pagoOk) {
    return (
      <CommonLayout>
        <div className={styles.page}>
          <div className={styles.card}>
            <div className={styles.successIcon}>✓</div>
            <h2 className={styles.title}>¡Pago confirmado!</h2>
            <p className={styles.hint}>Tu turno quedó reservado y confirmado.</p>
            <button className={styles.btnPrimary} onClick={() => navigate("/profesionales")}>
              Volver al inicio
            </button>
          </div>
        </div>
      </CommonLayout>
    );
  }

  return (
    <CommonLayout>
      <div className={styles.page}>
        <div className={styles.card}>
          {/* Header simulando MP */}
          <div className={styles.mpHeader}>
            <span className={styles.mpLogo}>Pago seguro</span>
          </div>

          {/* Resumen del turno */}
          <div className={styles.resumen}>
            <h3 className={styles.resumenTitle}>Resumen</h3>
            {turno && (
              <>
                <div className={styles.resumenRow}>
                  <span className={styles.resumenLabel}>Profesional</span>
                  <span>
                    {turno.profesionalNombre} {turno.profesionalApellido}
                  </span>
                </div>
                <div className={styles.resumenRow}>
                  <span className={styles.resumenLabel}>Fecha</span>
                  <span>
                    {formatFecha(turno.fecha)} · {turno.bloqueHorario}hs
                  </span>
                </div>
                {turno.servicioNombre && (
                  <div className={styles.resumenRow}>
                    <span className={styles.resumenLabel}>Servicio</span>
                    <span>{turno.servicioNombre}</span>
                  </div>
                )}
                {turno.servicioPrecio != null && (
                  <>
                    <div className={styles.resumenRow}>
                      <span className={styles.resumenLabel}>Precio total</span>
                      <span>${turno.servicioPrecio.toLocaleString("es-AR")}</span>
                    </div>
                    <div className={`${styles.resumenRow} ${styles.resumenTotal}`}>
                      <span className={styles.resumenLabel}>Seña ({PAGO.DEPOSIT_PERCENTAGE * 100}%)</span>
                      <span className={styles.precio}>${(turno.servicioPrecio * PAGO.DEPOSIT_PERCENTAGE).toLocaleString("es-AR")}</span>
                    </div>
                    <p className={styles.depositNote}>El resto se abona en el turno.</p>
                  </>
                )}
              </>
            )}
          </div>

          {/* Datos de tarjeta falsos */}
          <div className={styles.form}>
            <label className={styles.label}>Número de tarjeta</label>
            <input
              className={styles.input}
              type="text"
              inputMode="numeric"
              placeholder="123456789012"
              value={cardNumber}
              onChange={(e) => handleCardNumberChange(e.target.value)}
              onBlur={(e) => handleCardNumberChange(e.target.value)}
              maxLength={12}
            />
            {cardNumberError && <p className={styles.fieldError}>{cardNumberError}</p>}
            <div className={styles.row}>
              <div style={{ flex: 1 }}>
                <label className={styles.label}>Vencimiento</label>
                <input
                  className={styles.input}
                  type="text"
                  placeholder="MM/AA"
                  value={cardExpiry}
                  onChange={(e) => handleCardExpiryChange(e.target.value)}
                  onBlur={(e) => handleCardExpiryChange(e.target.value)}
                  maxLength={5}
                />
                {cardExpiryError && <p className={styles.fieldError}>{cardExpiryError}</p>}
              </div>
              <div style={{ flex: 1 }}>
                <label className={styles.label}>CVV</label>
                <input
                  className={styles.input}
                  type="text"
                  placeholder="123"
                  value={cardCvv}
                  onChange={(e) => handleCardCvvChange(e.target.value)}
                  onBlur={(e) => handleCardCvvChange(e.target.value)}
                  maxLength={4}
                />
                {cardCvvError && <p className={styles.fieldError}>{cardCvvError}</p>}
              </div>
            </div>
            <label className={styles.label}>Titular</label>
            <input
              className={styles.input}
              type="text"
              placeholder="Nombre como aparece en la tarjeta"
              value={cardHolder}
              onChange={(e) => handleCardHolderChange(e.target.value)}
              onBlur={(e) => handleCardHolderChange(e.target.value)}
            />
            {cardHolderError && <p className={styles.fieldError}>{cardHolderError}</p>}
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <button
            className={styles.btnPrimary}
            onClick={confirmarPago}
            disabled={pagando || !formularioValido}
          >
            {pagando
              ? "Procesando..."
              : `Pagar seña $${turno?.servicioPrecio != null ? (turno.servicioPrecio * PAGO.DEPOSIT_PERCENTAGE).toLocaleString("es-AR") : ""}`}
          </button>

          <button className={styles.btnSecondary} onClick={cancelarPago} disabled={pagando}>
            Cancelar
          </button>
        </div>
      </div>
    </CommonLayout>
  );
};


