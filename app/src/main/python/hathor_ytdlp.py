"""
Chaquopy-side helpers for yt-dlp.

Called from Kotlin via the chaquopy Python bridge. Returns plain JSON-serialisable dicts
so the Kotlin side does not need to traverse PyObject trees.

Public functions:
  enumerate_playlist(url) -> dict
  resolve_audio_url(video_id) -> dict | None
  ytdlp_version() -> str
  update_ytdlp() -> str  (returns the new version after pip install -U)
"""

from __future__ import annotations

import json
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

_AUDIO_OPTS = {
    "quiet": True,
    "no_warnings": True,
    "skip_download": True,
    "format": "bestaudio/best",
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


def resolve_audio_url(video_id: str) -> str:
    """Returns JSON: { url, ext, abr, vcodec, acodec } for the chosen bestaudio stream."""
    url = f"https://www.youtube.com/watch?v={video_id}"
    with yt_dlp.YoutubeDL(_AUDIO_OPTS) as ydl:
        info = ydl.extract_info(url, download=False)

    if info is None:
        return json.dumps({"error": "extract_info returned None"})

    # When format is bestaudio/best, yt-dlp picks one and surfaces it on the top-level dict.
    return json.dumps(
        {
            "url": info.get("url"),
            "ext": info.get("ext"),
            "abr": info.get("abr"),
            "acodec": info.get("acodec"),
            "vcodec": info.get("vcodec"),
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
