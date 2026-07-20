import Icon from "./Icon";
import "./UpiQrModal.css";

export default function UpiQrModal({ show, onClose, qrData }) {
  if (!show || !qrData) return null;

  return (
    <div className="upi-qr-modal-overlay" onClick={onClose} role="presentation">
      <div aria-modal="true" className="upi-qr-modal-content" onClick={(e) => e.stopPropagation()} role="dialog">
        <button aria-label="Close UPI payment" className="upi-qr-close-btn" onClick={onClose} type="button">
          <Icon name="close" size={18} />
        </button>
        <h2 className="upi-qr-title">Pay with UPI</h2>
        <p className="upi-qr-subtitle">
          Scan the QR code with your UPI app (GPay, PhonePe, Paytm, etc.)
        </p>
        
        <div className="upi-qr-code-container">
          <img
            src={`data:image/png;base64,${qrData.qrCodeBase64}`}
            alt="UPI QR Code"
            className="upi-qr-image"
          />
        </div>

        <div className="upi-qr-details">
          <p className="upi-qr-amount">
            <strong>Amount:</strong> ₹{qrData.amount}
          </p>
          <p className="upi-qr-vpa">
            <strong>UPI ID:</strong> {qrData.vpa}
          </p>
          <p className="upi-qr-order">
            <strong>Order ID:</strong> {qrData.orderId}
          </p>
        </div>

        <div className="upi-qr-instructions">
          <h3>How to pay:</h3>
          <ol>
            <li>Open your UPI app (GPay, PhonePe, Paytm, etc.)</li>
            <li>Scan the QR code above</li>
            <li>Confirm the payment details</li>
            <li>Complete the payment</li>
            <li>Credits will be added automatically after verification</li>
          </ol>
        </div>

        <button className="upi-qr-done-btn btn btn-primary" onClick={onClose} type="button">
          Done
        </button>
      </div>
    </div>
  );
}
