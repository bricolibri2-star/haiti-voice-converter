*** Begin Patch
*** Update File: backend/app/main.py
@@
 from fastapi.responses import FileResponse
+
+from app.audio_effect import transform_audio
@@
 def _run_engine(source: Path, refs_manifest: Path, output: Path, fidelity: float, pitch: float) -> None:
@@
-    raw_command = os.getenv("VOICE_CONVERSION_COMMAND")
-    if not raw_command:
-        raise HTTPException(status_code=503, detail="Le moteur vocal n’est pas configuré ; aucune conversion factice ne sera créée.")
+    raw_command = os.getenv("VOICE_CONVERSION_COMMAND")
+    if not raw_command:
+        try:
+            transform_audio(source, output, pitch)
+        except ValueError as exc:
+            raise HTTPException(status_code=422, detail="Le réglage de hauteur est invalide.") from exc
+        except RuntimeError as exc:
+            raise HTTPException(status_code=503, detail=str(exc)) from exc
+        return
@@
 def _run_engine(source: Path, refs_manifest: Path, output: Path, fidelity: float, pitch: float) -> None:
*** End Patch