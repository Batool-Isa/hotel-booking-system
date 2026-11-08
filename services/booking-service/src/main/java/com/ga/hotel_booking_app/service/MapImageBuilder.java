package com.ga.hotel_booking_app.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class MapImageBuilder {

    interface TileLoader {
        BufferedImage load(int zoom, int x, int y);
    }

    private static final int TILE = 256;
    private static final int ZOOM = 15;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;

    public static byte[] build(double lat, double lng) {
        return build(lat, lng, MapImageBuilder::downloadTile);
    }

    static byte[] build(double lat, double lng, TileLoader loader) {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            int n = 1 << ZOOM;
            double latRad = Math.toRadians(lat);
            double worldX = (lng + 180.0) / 360.0 * n * TILE;
            double worldY = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n * TILE;

            double left = worldX - WIDTH / 2.0;
            double top = worldY - HEIGHT / 2.0;
            int tx0 = (int) Math.floor(left / TILE);
            int tx1 = (int) Math.floor((left + WIDTH - 1) / TILE);
            int ty0 = (int) Math.floor(top / TILE);
            int ty1 = (int) Math.floor((top + HEIGHT - 1) / TILE);

            List<int[]> coords = new ArrayList<>();
            List<Future<BufferedImage>> futures = new ArrayList<>();
            for (int ty = ty0; ty <= ty1; ty++) {
                for (int tx = tx0; tx <= tx1; tx++) {
                    final int x = Math.floorMod(tx, n);
                    final int y = Math.max(0, Math.min(n - 1, ty));
                    coords.add(new int[]{tx, ty});
                    futures.add(pool.submit(() -> loader.load(ZOOM, x, y)));
                }
            }

            BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xE5E7EB));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < futures.size(); i++) {
                BufferedImage tile = futures.get(i).get(4, TimeUnit.SECONDS);
                if (tile == null) {
                    return null;
                }
                int px = (int) Math.round(coords.get(i)[0] * TILE - left);
                int py = (int) Math.round(coords.get(i)[1] * TILE - top);
                g.drawImage(tile, px, py, null);
            }

            drawMarker(g, WIDTH / 2, HEIGHT / 2);
            drawAttribution(g);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void drawMarker(Graphics2D g, int cx, int cy) {
        double r = 15;
        double headY = cy - 30;
        Path2D tail = new Path2D.Double();
        tail.moveTo(cx - r * 0.85, headY + r * 0.5);
        tail.lineTo(cx, cy);
        tail.lineTo(cx + r * 0.85, headY + r * 0.5);
        tail.closePath();
        Area pin = new Area(new Ellipse2D.Double(cx - r, headY - r, r * 2, r * 2));
        pin.add(new Area(tail));

        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(cx - 9, cy - 4, 18, 8));
        g.setColor(new Color(0xE11D48));
        g.fill(pin);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(pin);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 6, headY - 6, 12, 12));
    }

    private static void drawAttribution(Graphics2D g) {
        String text = "© OpenStreetMap contributors";
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 12;
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(WIDTH - w, HEIGHT - 20, w, 20);
        g.setColor(new Color(0x333333));
        g.drawString(text, WIDTH - w + 6, HEIGHT - 6);
    }

    private static BufferedImage downloadTile(int zoom, int x, int y) {
        try {
            URL url = new URL("https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setRequestProperty("User-Agent", "VibeStay/1.0 (hotel booking student project)");
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (var in = conn.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
