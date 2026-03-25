<?php
// ============================================================
//  get_alerts.php — Return alert history for a device
//  GET /api/get_alerts.php?device_id=ESP32_001&limit=20
//  POST /api/get_alerts.php  Body: { device_id, mark_read: true }
// ============================================================
require_once 'config.php';

$db = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Mark alerts as read
    $data      = getRequestBody();
    $device_id = trim($data['device_id'] ?? '');
    if (empty($device_id)) {
        jsonResponse(['success' => false, 'error' => 'device_id required'], 400);
    }
    $db->prepare("UPDATE alerts SET is_read = 1 WHERE device_id = :device_id")
       ->execute([':device_id' => $device_id]);
    jsonResponse(['success' => true, 'message' => 'Alerts marked as read']);
}

// GET — fetch alerts
$device_id = $_GET['device_id'] ?? '';
$limit     = min((int)($_GET['limit'] ?? 50), 200);

if (empty($device_id)) {
    jsonResponse(['success' => false, 'error' => 'device_id required'], 400);
}

$stmt = $db->prepare("
    SELECT a.id, a.alert_type, a.latitude, a.longitude, a.is_read, a.timestamp,
           d.child_name
    FROM alerts a
    JOIN devices d ON d.device_id = a.device_id
    WHERE a.device_id = :device_id
    ORDER BY a.timestamp DESC
    LIMIT :lim
");
$stmt->bindValue(':device_id', $device_id);
$stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
$stmt->execute();
$alerts = $stmt->fetchAll();

// Unread count
$unread = $db->prepare("SELECT COUNT(*) FROM alerts WHERE device_id = :d AND is_read = 0");
$unread->execute([':d' => $device_id]);
$unreadCount = (int) $unread->fetchColumn();

jsonResponse([
    'success'      => true,
    'device_id'    => $device_id,
    'alerts'       => $alerts,
    'total'        => count($alerts),
    'unread_count' => $unreadCount,
]);
