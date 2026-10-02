import os
import datetime
import requests

bot_token = os.environ["BOT_TOKEN"]
chat_id = os.environ["CHAT_ID"]
repo = os.environ.get("GITHUB_REPOSITORY")
workflow_file = os.environ.get("WORKFLOW_FILE")
github_token = os.environ.get("GITHUB_TOKEN")

default_minutes = 9
est_minutes = default_minutes

# Ambil riwayat run sebelumnya dari GitHub API
if github_token and repo:
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {github_token}",
        "X-GitHub-Api-Version": "2022-11-28"
    }
    
    # Targetkan workflow spesifik (debug / release)
    urls = []
    if workflow_file:
        urls.append(f"https://api.github.com/repos/{repo}/actions/workflows/{workflow_file}/runs?status=success&per_page=5")
    urls.append(f"https://api.github.com/repos/{repo}/actions/runs?status=success&per_page=5")
    
    durations = []
    for url in urls:
        try:
            res = requests.get(url, headers=headers, timeout=10)
            if res.status_code == 200:
                runs = res.json().get("workflow_runs", [])
                for r in runs:
                    start_str = r.get("run_started_at")
                    end_str = r.get("updated_at")
                    if start_str and end_str:
                        start = datetime.datetime.fromisoformat(start_str.replace("Z", "+00:00"))
                        end = datetime.datetime.fromisoformat(end_str.replace("Z", "+00:00"))
                        dur_seconds = (end - start).total_seconds()
                        if dur_seconds > 30:  # Abaikan build yang gagal instan/dibatalkan
                            durations.append(dur_seconds)
                if durations:
                    break
        except Exception as e:
            print(f"Warning: Gagal mengambil riwayat run ({e})")
            
    if durations:
        # Hitung rata-rata dan bulatkan ke menit
        est_minutes = max(1, round(sum(durations) / len(durations) / 60))

# Format pesan sesuai permintaan
message = (
    f"🚀Building..\n"
    f"The results will be sent in {est_minutes} minutes If there is no error"
)

# Kirim pesan ke Telegram
response = requests.post(
    f"https://api.telegram.org/bot{bot_token}/sendMessage",
    data={"chat_id": chat_id, "text": message}
)

if response.status_code == 200:
    print(f"Notifikasi awal terkirim! Estimasi: {est_minutes} menit.")
else:
    print(f"Gagal mengirim pesan Telegram: {response.text}")