import io
from contextlib import redirect_stdout
import datetime
import toml
import openai

from SSPUBot.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def RemonstrationOfficial(content):
    for i in config["LNLPM"]["ai_model"]:
        try:
            print(f"Using model: {i} for remonstration official.")
            remonstration_section = config['SSPUWebSiteUsesRequestsInfoSource']['system_content'][
                'RemonstrationOfficial']
            keys_view = remonstration_section.keys()
            keys_list = list(keys_view)
            code_content = ""
            for j in keys_list:
                if j in ["fix_code_bug", "fix_code_bug_retry_max"] or j.endswith("_non_python_code"):
                    continue
                print(f"Processing section: {j},", "using model:", i)
                current_content = ""
                if remonstration_section[j]["need_python_result"]:
                    if code_content != "":
                        current_content = remonstration_section[j][
                                              "content"] + "\n上一次对话的代码的输出结果为：\n" + code_content
                    else:
                        current_content = remonstration_section[j + "_non_python_code"]["content"] + "\n上一次对话没有代码输出结果。"
                else:
                    current_content = remonstration_section[j]["content"]
                if remonstration_section[j]["use_python_code"]:
                    codeContent = get_LNLPM_response(
                        prompt=content,
                        systemContent=current_content,
                        model=i
                    )
                    print("Executing the following code:")
                    print(codeContent)
                    codeContent = codeContent.replace("```python", "").replace("```", "")
                    code_content = ""
                    for _ in range(remonstration_section["fix_code_bug_retry_max"]):  # Retry mechanism
                        try:
                            output_buffer = io.StringIO()
                            with redirect_stdout(output_buffer):
                                exec(codeContent)
                            code_content = output_buffer.getvalue()
                            break
                        except Exception as e:
                            code_content = f"代码执行时出错，错误信息为：{str(e)}。请修正代码后重新生成。"
                            print(code_content)
                            print(f"Processing section: {j},", "using model:", i)
                            codeContent = get_LNLPM_response(
                                prompt=code_content,
                                systemContent=remonstration_section["fix_code_bug"]["content"],
                                model=i
                            )
                            codeContent = codeContent.replace("```python", "").replace("```", "")
                            continue
                else:
                    content = get_LNLPM_response(
                        prompt=content,
                        systemContent=current_content,
                        model="qwen/qwen-2.5-vl-7b-instruct:free"
                    )
            return content
        except openai.RateLimitError:
            print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
            continue
    return "Error: All models rate limited or failed."
