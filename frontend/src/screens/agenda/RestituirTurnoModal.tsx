import { formatFecha } from "../../utils/formatters";
import styles from "../AgendaScreen.module.css";

type TurnoDTO = {
  id: number;
  fecha: string;
  bloqueHorario: string;
  clienteNombre: string | null;
  nombreCliente: string | null;
  clienteApellido: string | null;
};

type Props = {
  turno: TurnoDTO;
  onRestituir: (turnoId: number) => void;
  onBloquear: (turno: TurnoDTO, authedFetch: (url: string, options?: RequestInit) => Promise<Response>) => Promise<void>;
  onClose: () => void;
  authedFetch: (url: string, options?: RequestInit) => Promise<Response>;
};

export const RestituirTurnoModal = ({ turno, onRestituir, onBloquear, onClose, authedFetch }: Props) => {
  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <h3 className={styles.modalTitle}>Gestionar turno cancelado</h3>
        <p className={styles.modalText}>
          {turno.clienteNombre ?? turno.nombreCliente ?? "—"} {turno.clienteApellido ?? ""} — {formatFecha(turno.fecha)}{" "}
          {turno.bloqueHorario}hs
        </p>
        <p className={styles.modalText}>¿Qué querés hacer con este horario?</p>
        <div className={styles.modalActions}>
          <button className={styles.btnMantener} onClick={() => { onRestituir(turno.id); onClose(); }}>
            Liberar (otro cliente puede reservar)
          </button>
          <button className={styles.btnCancelarTurnos} onClick={async () => { await onBloquear(turno, authedFetch); onClose(); }}>
            Bloquear (el horario queda no disponible)
          </button>
        </div>
        <button className={styles.btnMantener} style={{ marginTop: "0.25rem" }} onClick={onClose}>
          Volver
        </button>
      </div>
    </div>
  );
};
