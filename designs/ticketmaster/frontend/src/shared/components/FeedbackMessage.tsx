interface FeedbackMessageProps {
  variant?: "success" | "error" | "neutral";
  message: string;
}

export function FeedbackMessage({ variant = "neutral", message }: FeedbackMessageProps) {
  return <p className={`feedback-message feedback-message--${variant}`}>{message}</p>;
}
