# 360 panoramas

Put equirectangular 360 photos (2:1 ratio, for example 4096x2048 JPG) in this folder and list them in `index.json`.

```json
{ "hotelId": 1, "file": "hotel-1-lobby.jpg", "title": "Lobby" }
```

- A hotel with its own entries shows only those.
- Entries with `"hotelId": null` are shown for hotels that have none of their own. `sample-terrace.jpg` is a demo image of this kind and is marked "Sample image" in the viewer through `"sample": true`.
- Rebuild the frontend image (push to main) so the files are included.

## Credits

The sample image is tone-mapped from the CC0 "Venice Sunset" HDRI by Greg Zaal, from Poly Haven (polyhaven.com).
