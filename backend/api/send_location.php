<?php
// ============================================================
//  send_location.php — Receive GPS data from ESP32
//  POST /api/send_location.php
//  Body: { device_id, latitude, longitude, timestamp,
//          speed_kmph, satellites, hdop }
// ============================================================
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['success' => false, 'error' => 'POST required'], 405);
}

$data = getRequestBody();

// Validate required fields
$required = ['device_id', 'latitude', 'longitude'];
foreach ($required as $field) {
    if (empty($data[$field])) {
        jsonResponse(['success' => false, 'error' => "Missing: $field"], 400);
    }
}

$device_id  = trim($data['device_id']);
$lat        = (float) $data['latitude'];
$lng        = (float) $data['longitude'];
$speed      = isset($data['speed_kmph']) ? (float) $data['speed_kmph'] : 0;
$satellites = isset($data['satellites']) ? (int) $data['satellites'] : 0;
$hdop       = isset($data['hdop'])       ? (float) $data['hdop']      : 99.9;
$timestamp  = $data['timestamp'] ?? date('Y-m-d H:i:s');

$db = getDB();

// Insert location record
$stmt = $db->prepare("
    INSERT INTO locations (device_id, latitude, longitude, speed_kmph, satellites, hdop, timestamp)
    VALUES (:device_id, :lat, :lng, :speed, :satellites, :hdop, :ts)
");
$stmt->execute([
    ':device_id'  => $device_id,
    ':lat'        => $lat,
    ':lng'        => $lng,
    ':speed'      => $speed,
    ':satellites' => $satellites,
    ':hdop'       => $hdop,
    ':ts'         => $timestamp,
]);

// Update device last_seen
$db->prepare("
    UPDATE devices SET last_seen = NOW() WHERE device_id = :device_id
")->execute([':device_id' => $device_id]);

// Keep only last 1000 records per device (auto-cleanup)
$db->prepare("
    DELETE FROM locations
    WHERE device_id = :device_id
      AND id NOT IN (
          SELECT id FROM (
              SELECT id FROM locations
              WHERE device_id = :device_id2
              ORDER BY timestamp DESC
              LIMIT 1000
          ) tmp
      )
")->execute([':device_id' => $device_id, ':device_id2' => $device_id]);

jsonResponse([
    'success'   => true,
    'message'   => 'Location stored',
    'device_id' => $device_id,
    'lat'       => $lat,
    'lng'       => $lng,
]);
