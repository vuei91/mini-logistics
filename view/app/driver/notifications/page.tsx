"use client";

import { NotificationList } from "@/components/NotificationList";
import { RoleGuard } from "@/components/RoleGuard";

export default function DriverNotificationsPage() {
  return (
    <RoleGuard role="DRIVER">
      <NotificationList />
    </RoleGuard>
  );
}
