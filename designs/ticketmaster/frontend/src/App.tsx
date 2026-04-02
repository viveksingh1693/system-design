import { Navigate, Route, Routes } from "react-router-dom";
import { BookingDeskPage } from "./features/bookings/pages/BookingDeskPage";
import { DiscoveryPage } from "./features/discovery/pages/DiscoveryPage";
import { OperationsPage } from "./features/operations/pages/OperationsPage";
import { AppShell } from "./shared/components/AppShell";

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<DiscoveryPage />} />
        <Route path="/operations" element={<OperationsPage />} />
        <Route path="/bookings" element={<BookingDeskPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
