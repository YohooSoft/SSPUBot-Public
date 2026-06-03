import toml
import openai

from SSPUBot.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def check_tags_generate(tags):
    """
    :param tags: 标签列表
    :return: 检查标签是否重复，若有重复则去除重复标签并重新生成不重复的标签列表
    """
    print(f"Using model: free:Qwen3-30B-A3B for tag checking and generation.")
    try:
        html_content = get_LNLPM_response(
            prompt=f"""
            检查生成的标签是否重复，如果出现重复，请将重复的部分去除掉，然后重新生成一组不重复的标签。
            标签:
            {tags}
            """,
            systemContent=config["SSPUWebSiteUsesRequestsInfoSource"]["system_content"]["task4"],
            model="free:Qwen3-30B-A3B"
        )
        return html_content
    except openai.RateLimitError:
        print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
    return "Error: All models rate limited or failed."
