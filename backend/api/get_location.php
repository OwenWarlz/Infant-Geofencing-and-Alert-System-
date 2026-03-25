<?php
// ============================================================
//  get_location.php — Return latest GPS location for a device
//  GET /api/get_location.php?device_id=ESP32_001
//  GET /api/get_location.php?device_id=ESP32_001&history=50
// ============================================================
require_once 'config.php';

$device_id = $_GET['device_id'] ?? '';
$history   = isset($_GET['history']) ? min((int)$_GET['history'], 500) : 0;

if (empty($device_id)) {
    jsonResponse(['success' => false, 'error' => 'device_id required'], 400);
}

$db = getDB();

if ($history > 0) {
    // Return last N locations for trail display
    $stmt = $db->prepare("
        SELECT latitude, longitude, speed_kmph, satellites, timestamp
        FROM locations
        WHERE device_id = :device_id
        ORDER BY timestamp DESC
        LIMIT :limit
    ");
    $stmt->bindValue(':device_id', $device_id);
    $stmt->bindValue(':limit', $history, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll();

    jsonResponse([
        'success'   => true,
        'device_id' => $device_id,
        'trail'     => array_reverse($rows),
        'count'     => count($rows),
    ]);
} else {
    // Return single latest location
    $stmt = $db->prepare("
        SELECT l.latitude, l.longitude, l.speed_kmph, l.satellites, l.hdop, l.timestamp,
               d.child_name, d.battery_pct, d.last_seen
        FROM locations l
        JOIN devices d ON d.device_id = l.device_id
        WHERE l.device_id = :device_id
        ORDER BY l.timestamp DESC
        LIMIT 1
    ");
    $stmt->execute([':device_id' => $device_id]);
    $row = $stmt->fetch();

    if (!$row) {
        jsonResponse(['success' => false, 'error' => 'No location found'], 404);
    }

    // Check if device is online (last seen within 30s)
    $lastSeen = strtotime($row['last_seen'] ?? '0');
    $online   = (time() - $lastSeen) < 30;

    jsonResponse([
        'success'    => true,
        'device_id'  => $device_id,
        'child_name' => $row['child_name'],
        'latitude'   => (float) $row['latitude'],
        'longitude'  => (float) $row['longitude'],
        'speed_kmph' => (float) $row['speed_kmph'],
        'satellites' => (int)   $row['satellites'],
        'hdop'       => (float) $row['hdop'],
        'battery_pct'=> $row['battery_pct'],
        'timestamp'  => $row['timestamp'],
        'online'     => $online,
    ]);
}
