package top.mryan2005.sspubot.sspubotbackend.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/help")
@CrossOrigin(origins = "*")
public class HelpController {

    @GetMapping("/{module}")
    public ResponseEntity<Map<String, Object>> getModuleHelp(@PathVariable String module) {
        Map<String, Object> content = new HashMap<>();

        switch (module) {
            case "query":
                content.put("title", "查询模块");
                content.put("description", "SSPUBot提供强大的查询功能，帮助您快速找到所需的信息。");
                content.put("features", Arrays.asList(
                    "标题和关键词查询：支持按标题或关键词搜索相关内容",
                    "高级搜索：支持多条件组合搜索（通过来源和发布时间进行筛选），满足复杂查询需求"
                ));
                content.put("usage", Arrays.asList(
                    "在首页搜索框中输入要查询的内容",
                    "选择查询类型（字符、词组或例句）",
                    "点击搜索按钮或按Enter键",
                    "查看搜索结果并浏览详细信息"
                ));
                break;

            case "statistics":
                content.put("title", "统计模块");
                content.put("description", "SSPUBot的统计模块提供了全面的数据分析功能，帮助您了解系统使用情况和数据趋势。");
                content.put("features", Arrays.asList(
                    "各来源文件数量统计：按不同来源分类统计文件数量",
                    "每日发布文件数量：统计每日新增文件数",
                    "关键词词频统计：分析关键词在文件中的出现频率"
                ));
                content.put("usage", Arrays.asList(
                    "访问统计页面（/statistics）",
                    "选择要查看的统计类型",
                    "设置时间范围和筛选条件",
                    "查看统计图表和详细数据"
                ));
                break;

            case "ai":
                content.put("title", "AI模块");
                content.put("description", "SSPUBot集成了先进的AI聊天功能，可以与智能机器人进行对话，获取帮助和信息。");
                content.put("features", Arrays.asList(
                    "智能对话：与AI机器人进行自然语言对话",
                    "多机器人支持：可选择不同的AI机器人进行交互",
                    "上下文理解：AI能够理解对话上下文，提供连贯的回复",
                    "个性化配置：管理员可配置不同的AI机器人及其参数"
                ));
                content.put("usage", Arrays.asList(
                    "访问AI聊天页面（/chat）",
                    "选择要使用的AI机器人（如果有多个可选）",
                    "在输入框中输入您的问题或消息",
                    "点击发送按钮或按Enter键发送消息",
                    "查看AI的回复并继续对话"
                ));
                break;

            case "admin":
                content.put("title", "管理员模块");
                content.put("description", "管理员模块提供了系统管理的各项功能，仅限管理员用户访问。通过此模块，管理员可以管理用户、机器人、爬虫和同义词等系统资源。");
                content.put("features", Arrays.asList(
                    "用户管理：查看所有用户、封禁/解封用户账号",
                    "机器人管理：添加、编辑、删除AI机器人，设置默认机器人",
                    "爬虫管理：启动、停止爬虫任务，查看爬虫运行状态",
                    "同义词管理：添加、编辑、删除同义词条目"
                ));
                content.put("usage", Arrays.asList(
                    "访问管理员页面（/admin）",
                    "使用顶部标签页切换不同的管理功能",
                    "在用户管理中，可以封禁或解封用户",
                    "在机器人管理中，可以添加新机器人或编辑现有机器人配置",
                    "在爬虫管理中，可以启动、停止爬虫并查看运行详情",
                    "在同义词管理中，可以维护系统的同义词库"
                ));
                break;

            case "profile":
                content.put("title", "个人资料模块");
                content.put("description", "个人资料模块允许用户查看和编辑自己的个人信息，管理账号设置。");
                content.put("features", Arrays.asList(
                    "查看个人信息：显示名称、出生年月等信息",
                    "编辑资料：修改显示名称、出生年月等信息"
                ));
                content.put("usage", Arrays.asList(
                    "访问个人资料页面（/profile）",
                    "点击编辑按钮进入编辑模式",
                    "修改需要更新的信息",
                    "点击保存按钮保存更改",
                    "查看更新后的个人资料"
                ));
                break;

            case "settings":
                content.put("title", "设置模块");
                content.put("description", "设置模块提供了各种系统和用户偏好的配置选项，帮助用户个性化使用体验。");
                content.put("features", Arrays.asList(
                    "隐私设置：控制个人信息在Ai面前的可见性",
                    "账号安全：修改密码的安全选项"
                ));
                content.put("usage", Arrays.asList(
                    "访问设置页面（/settings）",
                    "浏览不同的设置分类",
                    "修改需要调整的设置项",
                    "点击保存按钮应用更改",
                    "根据需要可以随时返回修改设置"
                ));
                break;

            default:
                return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(content);
    }
}
