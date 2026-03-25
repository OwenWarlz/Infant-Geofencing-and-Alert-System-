<?php
// ============================================================
//  check_geofence.php — Point-in-Polygon check + alert logic
//  POST /api/check_geofence.php
//  Body: { device_id, latitude, longitude }
// ============================================================
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(['success' => false, 'error' => 'POST required'], 405);
}

$data = getRequestBody();

if (empty($data['device_id']) || !isset($data['latitude']) || !isset($data['longitude'])) {
    jsonResponse(['success' => false, 'error' => 'device_id, latitude, longitude required'], 400);
}

$device_id = trim($data['device_id']);
$lat       = (float) $data['latitude'];
$lng       = (float) $data['longitude'];

$db = getDB();

// Get active geofence for this device
$stmt = $db->prepare("
    SELECT id, name, boundary_coordinates
    FROM geofences
    WHERE device_id = :device_id AND is_active = 1
    ORDER BY created_at DESC
    LIMIT 1
");
$stmt->execute([':device_id' => $device_id]);
$fence = $stmt->fetch();

if (!$fence) {
    jsonResponse(['success' => true, 'status' => 'no_fence', 'message' => 'No geofence set']);
}

$polygon = json_decode($fence['boundary_coordinates'], true);
$inside  = pointInPolygon($lat, $lng, $polygon);
$status  = $inside ? 'inside' : 'outside';

// Check last alert to avoid duplicate alerts (only alert if status changed)
$lastAlert = $db->prepare("
    SELECT alert_type FROM alerts
    WHERE device_id = :device_id
    ORDER BY timestamp DESC
    LIMIT 1
");
$lastAlert->execute([':device_id' => $device_id]);
$lastRow = $lastAlert->fetch();
$lastType = $lastRow['alert_type'] ?? null;

$alertCreated = false;

// Only log alert if status changed from last alert
if ($lastType !== $status) {
    $db->prepare("
        INSERT INTO alerts (device_id, alert_type, latitude, longitude)
        VALUES (:device_id, :type, :lat, :lng)
    ")->execute([
        ':device_id' => $device_id,
        ':type'      => $status,
        ':lat'       => $lat,
        ':lng'       => $lng,
    ]);
    $alertCreated = true;

    // Optional: Send push notification via Firebase FCM
    if ($status === 'outside') {
        sendPushNotification($device_id, $db);
    }
}

jsonResponse([
    'success'       => true,
    'device_id'     => $device_id,
    'status'        => $status,          // "inside" or "outside"
    'fence_name'    => $fence['name'],
    'latitude'      => $lat,
    'longitude'     => $lng,
    'alert_created' => $alertCreated,
]);

// ════════════════════════════════════════════════════════════
// Ray Casting Algorithm — Point-in-Polygon
// Returns true if point (lat, lng) is inside the polygon
// polygon: array of ['lat' => ..., 'lng' => ...]
// ════════════════════════════════════════════════════════════
function pointInPolygon(float $lat, float $lng, array $polygon): bool {
    $n       = count($polygon);
    $inside  = false;
    $j       = $n - 1;

    for ($i = 0; $i < $n; $i++) {
        $xi = (float) $polygon[$i]['lng'];
        $yi = (float) $polygon[$i]['lat'];
        $xj = (float) $polygon[$j]['lng'];
        $yj = (float) $polygon[$j]['lat'];

        $intersect = (($yi > $lat) !== ($yj > $lat))
            && ($lng < ($xj - $xi) * ($lat - $yi) / ($yj - $yi) + $xi);

        if ($intersect) $inside = !$inside;
        $j = $i;
    }

    return $inside;
}

// ════════════════════════════════════════════════════════════
// Firebase Cloud Messaging Push Notification (optional)
// Requires FCM server key in config
// ════════════════════════════════════════════════════════════
function sendPushNotification(string $device_id, PDO $db): void {
    $FCM_SERVER_KEY = 'YOUR_FCM_SERVER_KEY'; // Set in config or here
    if ($FCM_SERVER_KEY === 'YOUR_FCM_SERVER_KEY') return; // skip if not configured

    // Get parent FCM token
    $stmt = $db->prepare("
        SELECT u.fcm_token, d.child_name
        FROM devices d
        JOIN users u ON u.id = d.parent_user_id
        WHERE d.device_id = :device_id
    ");
    $stmt->execute([':device_id' => $device_id]);
    $row = $stmt->fetch();

    if (!$row || empty($row['fcm_token'])) return;

    $payload = json_encode([
        'to' => $row['fcm_token'],
        'notification' => [
            'title' => '⚠️ Geofence Alert!',
            'body'  => $row['child_name'] . ' has left the safe zone!',
            'sound' => 'default',
        ],
        'data' => [
            'device_id' => $device_id,
            'type'      => 'geofence_exit',
        ],
    ]);

    $ch = curl_init('https://fcm.googleapis.com/fcm/send');
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'Authorization: key=' . $FCM_SERVER_KEY,
        ],
        CURLOPT_POSTFIELDS     => $payload,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 5,
    ]);
    curl_exec($ch);
    curl_close($ch);
}
