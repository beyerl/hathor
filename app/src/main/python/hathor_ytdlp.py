"""
Chaquopy-side helpers for yt-dlp.

Called from Kotlin via the chaquopy Python bridge. Returns plain JSON-serialisable dicts
so the Kotlin side does not need to traverse PyObject trees.

Public functions:
  enumerate_playlist(url) -> dict
  download_audio(video_id, out_dir) -> dict  (lets yt-dlp do the actual fetch)
  ytdlp_version() -> str
  update_ytdlp() -> str  (returns the new version after pip install -U)
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from typing import Any

import yt_dlp


_FLAT_OPTS = {
    "quiet": True,
    "no_warnings": True,
    "extract_flat": True,
    "skip_download": True,
    "ignoreerrors": True,
}


def ytdlp_version() -> str:
    return yt_dlp.version.__version__


def enumerate_playlist(url: str) -> str:
    """JSON-encoded dict with playlist metadata + entries. Matches reference script #2."""
    with yt_dlp.YoutubeDL(_FLAT_OPTS) as ydl:
        info = ydl.extract_info(url, download=False)

    if info is None:
        return json.dumps({"error": "extract_info returned None"})

    entries: list[dict[str, Any]] = []
    for raw in info.get("entries", []) or []:
        if not raw or not raw.get("id"):
            continue
        if raw.get("is_live"):
            continue
        entries.append(
            {
                "id": raw.get("id"),
                "title": raw.get("title") or "",
                "uploader": raw.get("uploader") or raw.get("channel") or "",
                "duration": raw.get("duration"),
                "thumbnail": raw.get("thumbnail"),
                "release_year": raw.get("release_year"),
                "upload_date": raw.get("upload_date"),
                "genre": raw.get("genre"),
                "ytdlp_artist": raw.get("artist"),
                "ytdlp_track": raw.get("track"),
            }
        )

    return json.dumps(
        {
            "id": info.get("id"),
            "title": info.get("title") or "",
            "uploader": info.get("uploader") or "",
            "thumbnail": info.get("thumbnail"),
            "entries": entries,
        }
    )


def download_audio(video_id: str, out_dir: str) -> str:
    """Downloads the bestaudio stream into out_dir and returns JSON with the file path.

    Letting yt-dlp do the fetch is what handles n-sig deciphering, throttling
    workarounds, and DASH/HLS reassembly — a plain HTTP GET on info["url"] stalls
    or returns a manifest for many YouTube/YouTube Music streams.
    """
    os.makedirs(out_dir, exist_ok=True)
    url = f"https://www.youtube.com/watch?v={video_id}"

    opts = {
        "quiet": True,
        "no_warnings": True,
        "format": "bestaudio/best",
        "noplaylist": True,
        "outtmpl": os.path.join(out_dir, "%(id)s.%(ext)s"),
        "restrictfilenames": True,
        "overwrites": True,
        "retries": 5,
        "fragment_retries": 5,
    }

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=True)
    except Exception as e:
        return json.dumps({"error": f"{type(e).__name__}: {e}"})

    if info is None:
        return json.dumps({"error": "extract_info returned None"})

    # requested_downloads is the most reliable source of the resulting file path.
    requested = info.get("requested_downloads") or []
    filepath = (requested[0].get("filepath") if requested else None) or info.get("filepath")
    if not filepath:
        # Fall back to constructing it from outtmpl evaluation.
        ext = info.get("ext") or "m4a"
        filepath = os.path.join(out_dir, f"{info.get('id', video_id)}.{ext}")

    return json.dumps(
        {
            "path": filepath,
            "ext": info.get("ext"),
            "duration": info.get("duration"),
        }
    )


def update_ytdlp() -> str:
    """pip install -U yt-dlp inside the Chaquopy venv. Returns new version string."""
    subprocess.check_call(
        [sys.executable, "-m", "pip", "install", "-U", "yt-dlp"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    # Re-import to pick up the new module; on Android this requires app restart for full effect,
    # but the version string is updated immediately in pip's metadata.
    import importlib

    importlib.reload(yt_dlp)
    return yt_dlp.version.__version__
