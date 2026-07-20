import { Link } from "react-router-dom";
import Icon from "./Icon";

const FEATURES = [
  "Create text, images, and video in one workspace",
  "Organize reusable prompts and creative assets",
  "Collaborate with clear usage and billing controls",
];

export default function AuthShell({ eyebrow, title, description, children, footer }) {
  return (
    <div className="auth-shell">
      <section className="auth-showcase" aria-label="AI Studio overview">
        <Link className="auth-brand" to="/login">
          <span><Icon name="sparkles" size={21} /></span>
          <strong>AI Studio</strong>
        </Link>

        <div className="auth-showcase-copy">
          <p className="auth-showcase-kicker">Your creative operating system</p>
          <h2>Move from a blank page to production-ready content.</h2>
          <p>
            A focused workspace for building, organizing, and publishing AI-assisted
            content across your team.
          </p>

          <ul className="auth-feature-list">
            {FEATURES.map((feature) => (
              <li key={feature}>
                <span><Icon name="check" size={15} strokeWidth={2.3} /></span>
                {feature}
              </li>
            ))}
          </ul>
        </div>

        <div className="auth-proof">
          <div className="auth-avatar-stack" aria-hidden="true">
            <span>AR</span><span>MK</span><span>JL</span>
          </div>
          <p><strong>Built for focused teams</strong><br />One workspace, every format.</p>
        </div>
      </section>

      <main className="auth-main">
        <Link className="auth-mobile-brand" to="/login">
          <span><Icon name="sparkles" size={19} /></span>
          <strong>AI Studio</strong>
        </Link>
        <div className="auth-card">
          <header className="auth-header">
            {eyebrow && <p>{eyebrow}</p>}
            <h1>{title}</h1>
            <span>{description}</span>
          </header>
          {children}
          {footer && <footer className="auth-footer">{footer}</footer>}
        </div>
        <p className="auth-legal">Secure access · Your credentials stay private</p>
      </main>
    </div>
  );
}
