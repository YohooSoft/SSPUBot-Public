import toml
import openai

from SpiderNew.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def someone_who_cleans_up_the_mess(content, theMess):
    for i in config["LNLPM"]["ai_model"]:
        try:
            print(f"Using model: {i} for someone who cleans up the mess.")
            html_content = get_LNLPM_response(
                prompt=f"""
现在这篇文章被驳回了，你是作为擦屁股的人，你需要按照下面给出的驳回原因修改文章内容，修改后的文章内容需要符合驳回原因的要求。
驳回原因：{theMess}
文章内容：{content}
""",
                systemContent=config["SSPUWebSiteUsesRequestsInfoSource"]["system_content"]["task2"],
                # TODO: 需要修改systemContent
                model=i
            )
            return html_content
        except openai.RateLimitError:
            print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
            continue
    return "Error: All models rate limited or failed."
