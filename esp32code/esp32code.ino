// ============================================================
//  INFANT GEOFENCING ALERT SYSTEM — ESP32 + NEO-6M GPS
//  Sends GPS coordinates to PHP backend every 1 second
// ============================================================

#include <TinyGPSPlus.h>
#include <HardwareSerial.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

// ── WiFi Credentials ─────────────────────────────────────────
const char* WIFI_SSID     = "wifissid";
const char* WIFI_PASSWORD = "password";

// ── Backend Server ───────────────────────────────────────────
const char* SERVER_URL    = "http://server_ip/infant_geofence/api/send_location.php";
const char* GEOFENCE_URL  = "http://server_ip/infant_geofence/api/check_geofence.php";

// ── Device Identity ──────────────────────────────────────────
const char* DEVICE_ID     = "ESP32_001";

// ── GPS UART Pins ────────────────────────────────────────────
#define GPS_RX_PIN   16
#define GPS_TX_PIN   17
#define GPS_BAUD     115200   // check your baud rate

// ── Timing ───────────────────────────────────────────────────
#define SEND_INTERVAL_MS   1000   // send every 1 second
#define GPS_FIX_TIMEOUT_MS 60000  // wait up to 60s for fix

// ── Optional SOS Button ──────────────────────────────────────
#define SOS_PIN      0   // BOOT button on most ESP32 boards
#define LED_PIN      2   // onboard LED

// ────────────────────────────────────────────────────────────
HardwareSerial GPSSerial(2);
TinyGPSPlus    gps;

unsigned long lastSendTime    = 0;
unsigned long gpsFixAcquired  = 0;
bool          hasFix          = false;

// ════════════════════════════════════════════════════════════
void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(SOS_PIN, INPUT_PULLUP);

  Serial.println("\n=== Infant GeoFence Tracker Booting ===");

  // Start GPS
  GPSSerial.begin(GPS_BAUD, SERIAL_8N1, GPS_RX_PIN, GPS_TX_PIN);
  Serial.println("[GPS] Serial started at " + String(GPS_BAUD) + " baud");

  // Connect WiFi
  connectWiFi();
}

// ════════════════════════════════════════════════════════════
void loop() {
  // Feed GPS parser
  while (GPSSerial.available() > 0)
    gps.encode(GPSSerial.read());

  // Track fix acquisition
  if (!hasFix && gps.location.isValid()) {
    hasFix = true;
    gpsFixAcquired = millis();
    Serial.println("[GPS] Fix acquired!");
    blinkLED(3);
  }

  // SOS button (active LOW)
  if (digitalRead(SOS_PIN) == LOW) {
    Serial.println("[SOS] Button pressed!");
    sendSOS();
    delay(3000); // debounce
  }

  // Send location at interval
  if (hasFix && millis() - lastSendTime >= SEND_INTERVAL_MS) {
    lastSendTime = millis();
    sendLocation();
    checkGeofence();
  }

  // Warn if no GPS data coming in at all
  if (millis() > 15000 && gps.charsProcessed() < 10) {
    Serial.println("[ERROR] No GPS characters received — check wiring!");
    delay(5000);
  }

  // Status blink: fast = no fix, slow = ok
  static unsigned long lastBlink = 0;
  int blinkRate = hasFix ? 1000 : 200;
  if (millis() - lastBlink > blinkRate) {
    lastBlink = millis();
    digitalWrite(LED_PIN, !digitalRead(LED_PIN));
  }
}

// ════════════════════════════════════════════════════════════
void connectWiFi() {
  Serial.print("[WiFi] Connecting to " + String(WIFI_SSID));
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WiFi] Connected! IP: " + WiFi.localIP().toString());
    blinkLED(2);
  } else {
    Serial.println("\n[WiFi] Failed to connect. Will retry...");
  }
}

// ════════════════════════════════════════════════════════════
void sendLocation() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[WiFi] Disconnected, reconnecting...");
    connectWiFi();
    return;
  }

  // Build timestamp string
  char timestamp[25];
  snprintf(timestamp, sizeof(timestamp),
    "%04d-%02d-%02dT%02d:%02d:%02dZ",
    gps.date.year(), gps.date.month(), gps.date.day(),
    gps.time.hour(), gps.time.minute(), gps.time.second());

  // Build JSON payload
  StaticJsonDocument<256> doc;
  doc["device_id"]  = DEVICE_ID;
  doc["latitude"]   = String(gps.location.lat(), 8);
  doc["longitude"]  = String(gps.location.lng(), 8);
  doc["timestamp"]  = timestamp;
  doc["speed_kmph"] = gps.speed.isValid() ? gps.speed.kmph() : 0;
  doc["satellites"] = gps.satellites.isValid() ? gps.satellites.value() : 0;
  doc["hdop"]       = gps.hdop.isValid() ? gps.hdop.hdop() : 99.9;

  String payload;
  serializeJson(doc, payload);

  Serial.println("[HTTP] Sending: " + payload);

  HTTPClient http;
  http.begin(SERVER_URL);
  http.addHeader("Content-Type", "application/json");

  int code = http.POST(payload);
  if (code > 0) {
    String response = http.getString();
    Serial.println("[HTTP] Response (" + String(code) + "): " + response);
  } else {
    Serial.println("[HTTP] Error: " + http.errorToString(code));
  }
  http.end();
}

// ════════════════════════════════════════════════════════════
void checkGeofence() {
  if (WiFi.status() != WL_CONNECTED) return;

  StaticJsonDocument<128> doc;
  doc["device_id"] = DEVICE_ID;
  doc["latitude"]  = String(gps.location.lat(), 8);
  doc["longitude"] = String(gps.location.lng(), 8);

  String payload;
  serializeJson(doc, payload);

  HTTPClient http;
  http.begin(GEOFENCE_URL);
  http.addHeader("Content-Type", "application/json");

  int code = http.POST(payload);
  if (code == 200) {
    String response = http.getString();
    StaticJsonDocument<128> res;
    deserializeJson(res, response);

    const char* status = res["status"];
    if (strcmp(status, "outside") == 0) {
      Serial.println("[ALERT] Child is OUTSIDE the geofence!");
      blinkLED(10); // rapid blink = alert
    } else {
      Serial.println("[GEO] Child is inside safe zone.");
    }
  }
  http.end();
}

// ════════════════════════════════════════════════════════════
void sendSOS() {
  if (WiFi.status() != WL_CONNECTED) return;

  String sosUrl = String(SERVER_URL);
  sosUrl.replace("send_location", "send_sos");

  StaticJsonDocument<128> doc;
  doc["device_id"] = DEVICE_ID;
  doc["latitude"]  = gps.location.isValid() ? String(gps.location.lat(), 8) : "0";
  doc["longitude"] = gps.location.isValid() ? String(gps.location.lng(), 8) : "0";
  doc["sos"]       = true;

  String payload;
  serializeJson(doc, payload);

  HTTPClient http;
  http.begin(sosUrl);
  http.addHeader("Content-Type", "application/json");
  http.POST(payload);
  http.end();

  Serial.println("[SOS] SOS alert sent!");
  blinkLED(10);
}

// ════════════════════════════════════════════════════════════
void blinkLED(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(LED_PIN, HIGH); delay(100);
    digitalWrite(LED_PIN, LOW);  delay(100);
  }
}
