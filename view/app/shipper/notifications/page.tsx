"use client";

import { NotificationList } from "@/components/NotificationList";
import { RoleGuard } from "@/components/RoleGuard";

export default function ShipperNotificationsPage() {
  return (
    <RoleGuard role="SHIPPER">
      <NotificationList />
    </RoleGuard>
  );
}
