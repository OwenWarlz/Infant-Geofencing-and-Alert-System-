<?php
// ============================================================
//  set_geofence.php — Save polygon geofence from Flutter app
//  POST /api/set_geofence.php
//  Body: { device_id, name, coordinates: [{lat,lng}, ...] }
// ============================================================
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['success' => false, 'error' => 'POST required'], 405);
}

$data = getRequestBody();

if (empty($data['device_id']) || empty($data['coordinates'])) {
    jsonResponse(['success' => false, 'error' => 'device_id and coordinates required'], 400);
}

$device_id   = trim($data['device_id']);
$name        = $data['name'] ?? 'Safe Zone';
$coordinates = $data['coordinates']; // array of {lat, lng}

if (!is_array($coordinates) || count($coordinates) < 3) {
    jsonResponse(['success' => false, 'error' => 'Need at least 3 polygon points'], 400);
}

$db = getDB();

// Deactivate old geofences for this device
$db->prepare("UPDATE geofences SET is_active = 0 WHERE device_id = :device_id")
   ->execute([':device_id' => $device_id]);

// Insert new geofence
$stmt = $db->prepare("
    INSERT INTO geofences (device_id, name, boundary_coordinates, is_active)
    VALUES (:device_id, :name, :coords, 1)
");
$stmt->execute([
    ':device_id' => $device_id,
    ':name'      => $name,
    ':coords'    => json_encode($coordinates),
]);

jsonResponse([
    'success'   => true,
    'message'   => 'Geofence saved',
    'id'        => $db->lastInsertId(),
    'device_id' => $device_id,
    'points'    => count($coordinates),
]);
