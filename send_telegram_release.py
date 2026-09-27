import os
import requests

bot_token = os.environ["BOT_TOKEN"]
chat_id = os.environ["CHAT_ID"]
repo = os.environ["GITHUB_REPOSITORY"]
branch = os.environ["GITHUB_REF_NAME"]
commit = os.environ["GITHUB_SHA"][:7]
actor = os.environ["GITHUB_ACTOR"]
tag = os.environ.get("GITHUB_REF_NAME", "unknown")

caption = (
    f"<b>🚀 RELEASE BUILD SUCCESS</b>\n\n"
    f"<b>Repo:</b> {repo}\n"
    f"<b>Tag:</b> <code>{tag}</code>\n"
    f"<b>Commit:</b> <code>{commit}</code>\n"
    f"<b>Author:</b> {actor}"
)

with open("app/build/outputs/apk/release/app-release.apk", "rb") as apk:
    response = requests.post(
        f"https://api.telegram.org/bot{bot_token}/sendDocument",
        data={"chat_id": chat_id, "caption": caption, "parse_mode": "HTML"},
        files={"document": apk}
    )
    print(response.json())