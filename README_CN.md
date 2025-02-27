# SuperSonic (超音数)

**SuperSonic融合ChatBI和HeadlessBI打造新一代的数据分析平台**。通过SuperSonic的问答对话界面，用户能够使用自然语言查询数据，系统会选择合适的可视化图表呈现结果。SuperSonic不需要修改或复制数据，只需要在物理数据模型之上构建逻辑语义模型（指标/维度/实体的定义，以及他们的业务含义、相互间关系等），即可开启数据问答体验。与此同时，SuperSonic被设计为可插拔的框架，采用Java SPI机制来扩展定制功能。

<img src="./docs/images/supersonic_demo.gif" height="100%" width="100%" align="center"/>

## 项目动机

大型语言模型（LLMs）如ChatGPT的出现正在重塑信息检索的方式。在数据分析领域，学术界和工业界主要关注利用深度学习模型将自然语言查询转换为SQL查询。虽然一些工作显示出有前景的结果，但它们的可靠性还达不到生产可用的要求。

在我们看来，为了在实际场景发挥价值，有三个关键点：
1. 融合HeadlessBI，通过统一语义层封装底层数据细节（关联、键值、公式等），降低SQL生成的**复杂度**。
2. 通过一前一后的模式映射器和语义修正器，来缓解LLM常见的**幻觉**现象。
3. 设计启发式的规则，在一些特定场景提升语义解析的**效率**。

为了验证上述想法，我们开发了SuperSonic项目，并将其应用在实际的内部产品中。与此同时，我们将SuperSonic作为一个可扩展的框架开源，希望能够促进数据问答对话领域的进一步发展。

## 开箱即用的特性

- 内置ChatBI界面以便*业务用户*输入数据查询。
- 内置HeadlessBI界面以便*分析工程师*构建语义模型。
- 内置图形用户界面以便*系统管理员*管理第三方插件和对话助理。
- 支持文本输入的联想和查询问题的推荐。
- 支持多轮对话，根据语境自动切换上下文。
- 支持四级权限控制：主题域级、模型级、列级、行级。

## 易于扩展的组件

SuperSonic的整体架构和主流程如下图所示：

<img src="./docs/images/supersonic_components.png" height="65%" width="65%" align="center"/> 

- **模型知识库(Knowledge Base)：** 定期从语义模型中提取相关的模式信息，构建词典和索引，以便后续的模式映射。

- **模式映射器(Schema Mapper)：** 将自然语言文本在知识库中进行匹配，为后续的语义解析提供相关信息。

- **语义解析器(Semantic Parser)：** 理解用户查询并抽取语义信息，其由一组基于规则和基于模型的解析器组成，每个解析器可应对不同的特定场景。

- **语义修正器(Semantic Corrector)：** 检查语义信息的合法性，对不合法的信息做修正和优化处理。

- **语义解释器(Semantic Interpreter)：** 根据语义信息生成物理SQL执行查询。

- **问答插件(Chat Plugin)：** 通过第三方工具扩展功能。给定所有配置的插件及其功能描述和示例问题，大语言模型将选择最合适的插件。

## 快速体验

SuperSonic自带样例的语义模型和问答对话，只需以下三步即可快速体验：

- 从[release page](https://github.com/tencentmusic/supersonic/releases)下载预先构建好的发行包
- 运行 "assembly/bin/supersonic-daemon.sh start"启动standalone模式的Java服务
- 在浏览器访问http://localhost:9080 开启探索

## 如何构建和部署

请参考项目[wiki](https://github.com/tencentmusic/supersonic/wiki)。

## 微信联系方式

欢迎关注微信公众号：

<img src="./docs/images/supersonic_wechat_oa.png" height="50%" width="50%" align="center"/> 