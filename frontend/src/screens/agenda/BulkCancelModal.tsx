import { formatFecha } from "../../utils/formatters";
import styles from "../AgendaScreen.module.css";

type Props = {
  bulkFecha: string;
  bulkCanceling: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

export const BulkCancelModal = ({ bulkFecha, bulkCanceling, onConfirm, onClose }: Props) => {
  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <h3 className={styles.modalTitle}>Cancelar todos los turnos del día</h3>
        <p className={styles.modalText}>
          ¿Estás seguro de cancelar TODOS los turnos del {formatFecha(bulkFecha)}? Los clientes serán notificados.
          Esta acción no se puede deshacer.
        </p>
        <div className={styles.modalActions}>
          <button className={styles.btnCancelarTurnos} onClick={onConfirm} disabled={bulkCanceling}>
            {bulkCanceling ? "Cancelando..." : "Sí, cancelar todos"}
          </button>
          <button className={styles.btnMantener} onClick={onClose}>
            No, volver
          </button>
        </div>
      </div>
    </div>
  );
};
