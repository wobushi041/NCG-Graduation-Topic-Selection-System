import {PageContainer} from '@ant-design/pro-components';
import {useModel} from '@umijs/max';
import {Card, Carousel, Collapse, Divider, Tabs, theme, Typography} from 'antd';
import React from 'react';
import {Toc} from "@/pages/Toc";
// eslint-disable-next-line @typescript-eslint/no-unused-vars

const {Title, Paragraph, Text} = Typography;

/**
 * 每个单独的卡片，为了复用样式抽成了组件
 * @param param0
 * @returns
 */
const InfoCard: React.FC<{
  title: string;
  index: number;
  desc: string;
  href: string;
}> = ({title, href, index, desc}) => {
  const {useToken} = theme;

  const {token} = useToken();

  return (
    <div
      style={{
        backgroundColor: token.colorBgContainer,
        boxShadow: token.boxShadow,
        borderRadius: '8px',
        fontSize: '14px',
        color: token.colorTextSecondary,
        lineHeight: '22px',
        padding: '16px 19px',
        minWidth: '200px',
        flex: 1,
      }}
    >
      <div
        style={{
          display: 'flex',
          gap: '4px',
          alignItems: 'center',
        }}
      >
        <div
          style={{
            width: 48,
            height: 48,
            lineHeight: '22px',
            backgroundSize: '100%',
            textAlign: 'center',
            padding: '8px 16px 16px 12px',
            color: '#FFF',
            fontWeight: 'bold',
            backgroundImage:
              "url('https://gw.alipayobjects.com/zos/bmw-prod/daaf8d50-8e6d-4251-905d-676a24ddfa12.svg')",
          }}
        >
          {index}
        </div>
        <div
          style={{
            fontSize: '16px',
            color: token.colorText,
            paddingBottom: 8,
          }}
        >
          {title}
        </div>
      </div>
      <div
        style={{
          fontSize: '14px',
          color: token.colorTextSecondary,
          textAlign: 'justify',
          lineHeight: '22px',
          marginBottom: 8,
        }}
      >
        {desc}
      </div>
      <a href={href} target="_blank" rel="noreferrer">
        了解更多 {'>'}
      </a>
    </div>
  );
};

const Welcome: React.FC = () => {
  const {token} = theme.useToken();
  const {initialState} = useModel('@@initialState');
  return (
    <PageContainer title={false}>
      <div>
        <Card
          style={{
            borderRadius: 8,
          }}
          bodyStyle={{
            backgroundImage:
            //@ts-ignore
              initialState?.settings?.navTheme === 'realDark'
                ? 'background-image: linear-gradient(75deg, #1A1B1F 0%, #191C1F 100%)'
                : 'background-image: linear-gradient(75deg, #FBFDFF 0%, #F5F7FF 100%)',
          }}
        >
          <Title style={{textAlign: 'center'}}>欢迎使用广州南方学院毕设选题管理系统🎉</Title>
          <div
            style={{
              backgroundPosition: '100% -30%',
              backgroundRepeat: 'no-repeat',
              backgroundSize: '274px auto',
            }}
          >
            <p
              style={{
                fontSize: '14px',
                color: token.colorTextSecondary,
                lineHeight: '22px',
                marginTop: 16,
                marginBottom: 32,
                width: '65%',
              }}
            ></p>
            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: 16,
              }}
            >
              <InfoCard
                index={1}
                href="https://www.nfu.edu.cn/"
                title="了解广州南方学院"
                desc="广州南方学院（原中山大学南方学院）是经教育部批准设立的综合性应用型普通本科高校，致力于培养具有创新精神和实践能力的高素质人才。"
              />
              <InfoCard
                index={2}
                title="了解工学院"
                href="https://sece.nfu.edu.cn/"
                desc="工学院前身为电子通信与软件工程系，始建于 2006 年，承担电子信息、计算机与软件工程等领域的人才培养工作。"
              />
              <InfoCard
                index={3}
                title="查看项目源码"
                href="https://github.com/Lq0412/nfu-graduation-topic-selection"
                desc="项目仓库包含系统源码、本地运行说明、数据库结构及维护记录，可用于了解系统实现与后续建设计划。"
              />
            </div>
          </div>
          <Divider/>
          <Typography>
            <Title level={2}>使用指南</Title>
            <Tabs defaultActiveKey="1">
              <Tabs.TabPane tab="学生使用手册" key="1">
                <Paragraph>学生操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>
                        <Text code>持续关注并且大致浏览教师所出的毕设题目</Text>
                      </li>
                      <li>
                        <Text code>确认预选毕业设计题目</Text> 或 <Text code>取消预选毕业设计题目</Text>
                      </li>
                      <li>
                        <Text code>确认提交毕业设计题目</Text> 或 <Text code>取消提交毕业设计题目</Text>
                      </li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  学生可先浏览教师发布的 <Text code>毕业设计题目（以下简称“题目”）</Text>，并根据个人意向进行预选。
                  <Text underline>题目开放后</Text>，每名学生最多可预选 10 个题目；进入学生选题阶段后，需从可选题目中确认并提交一个最终题目。
                  提交成功后如需变更，请先按系统规则取消已提交题目，或联系指导教师协助处理。
                </Paragraph>
                <Paragraph>
                  本系统 <Text mark>支持在允许的阶段退选，请谨慎操作</Text>。若退选入口不可用或无法取消已提交题目，请联系对应指导教师处理。
                </Paragraph>
                <Paragraph>
                  选题过程中请勿使用连点器、自动刷新脚本等工具。<Text
                  style={{color: 'red'}}>系统检测到异常请求后可能临时限制账号访问</Text>，并影响正常选题安排。
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    defaultActiveKey={['1']}
                    items={[{
                      key: '1', label: '学生端简易演示过程', children:
                        <>
                          <Carousel
                            autoplay
                            dots={{className: 'custom-dots'}}
                            arrows
                            style={{margin: '0 auto'}}
                          >
                            <div>
                              <div style={{
                                textAlign: 'center',
                                marginTop: 12,
                                padding: '8px 12px',
                                background: '#f9f9f9',
                                borderRadius: 6,
                                color: '#555',
                                fontSize: 14,
                              }}>
                                1. 学生在题目开放前，点击菜单栏“学生选题 → 预选选题”，可以查看不同教师所发布的题目
                              </div>
                            </div>
                            <div>
                              <div style={{
                                textAlign: 'center',
                                marginTop: 12,
                                padding: '8px 12px',
                                background: '#f9f9f9',
                                borderRadius: 6,
                                color: '#555',
                                fontSize: 14,
                              }}>
                                2. 虽然题目暂时还没有开放，但是可以点击“预选题目”进行预选
                              </div>
                            </div>
                            <div>
                              <div style={{
                                textAlign: 'center',
                                marginTop: 12,
                                padding: '8px 12px',
                                background: '#f9f9f9',
                                borderRadius: 6,
                                color: '#555',
                                fontSize: 14,
                              }}>
                                3. 开放后，点击菜单栏“提交选题”，确认后只能选择一个题目
                              </div>
                            </div>
                            <div>
                              <div style={{
                                textAlign: 'center',
                                marginTop: 12,
                                padding: '8px 12px',
                                background: '#f9f9f9',
                                borderRadius: 6,
                                color: '#555',
                                fontSize: 14,
                              }}>
                                4. 点击菜单栏“查看选题”后可以查看最终选得题目的详细信息
                              </div>
                            </div>
                          </Carousel>
                        </>
                    }]}
                  />
                </Paragraph>
              </Tabs.TabPane>
              <Tabs.TabPane tab="教师使用手册" key="2">
                <Paragraph>教师操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>
                        <Text code>发布题目</Text> 或 <Text code>修改题目</Text>
                      </li>
                      <li>
                        <Text code>查看题目状态</Text>
                      </li>
                      <li>
                        <Text code>可以根据情况为已经审核通过的题目选择学生（双选）</Text>
                      </li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  教师应在规定时间内提交 <Text code>毕业设计题目（以下简称“题目”）</Text>，并及时关注审核状态。
                  不同教师的出题数量上限以系统配置为准；<Text underline>题目开放后</Text>，可根据选题规则查看并选择符合条件的学生。
                </Paragraph>
                <Paragraph>
                  本系统 <Text mark>支持在允许的阶段协助学生退选，请谨慎操作</Text>。若退选功能不可用，请联系选题负责人或系统管理员处理。
                </Paragraph>
                <Paragraph>
                  发布题目前可使用 <Text code>AI</Text> 校验工具辅助检查题目与近三年题目的相似情况，结果仅供参考。<Text
                  style={{color: 'red'}}>每位教师每日最多使用 30 次，请合理安排使用次数。</Text>
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    defaultActiveKey={['1']}
                    items={[{
                      key: '1',
                      label: '教师端简易演示过程',
                      children: (
                        <Carousel
                          autoplay
                          dots={{className: 'custom-dots'}}
                          arrows
                          style={{margin: '0 auto'}}
                        >
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              1. 点击菜单栏“教师发布 → 发布题目和修改题目”，这里可以看到自己发布的所有题目
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              2. 点击表格上方的“添加题目”，填写关于题目的信息表单，并且可以比较题目在 3 年内的相似程度（仅供参考）
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              3. 点击“提交”按钮后即可发布题目，等待选题负责人审核题目通过
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              4. 点击表格列“操作”区域的“编辑”，修改后点击“保存”，即可更新选题（也可“删除”后重新添加）
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              5. 若题目状态为“打回”，需根据打回理由修改后，点击“重新提交审核”，进入“待审核”状态
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              6. 题目处于“已发布”状态后，教师可点击“操作”区域的“选择学生”，进行双选
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              7. 点击“教师发布 → 查看选择自己的学生”查看情况，可视情况点击“退选”帮助学生取消选题
                            </div>
                          </div>
                        </Carousel>
                      )
                    }]}
                  />
                </Paragraph>
              </Tabs.TabPane>
              <Tabs.TabPane tab="选题负责人使用手册" key="3">
                <Paragraph>选题负责人操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>
                        <Text code>审核题目（通过题目、打回题目）</Text>
                      </li>
                      <li>
                        <Text code>查看本学院学生的选题情况</Text>
                      </li>
                      <li>
                        <Text code>快速导出选题情况表格文件</Text>
                      </li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  选题负责人应在 <Text underline>双选开始前</Text> 完成本选题组所有 <Text
                  code>毕业设计题目（以下简称“题目”）</Text> 的审核。被打回的题目可由教师根据意见修改后重新提交。
                </Paragraph>
                <Paragraph>
                  本系统支持选题负责人在满足条件时切换为教师身份进行出题。若右上角菜单中<Text underline>未显示“切换身份”入口</Text>，说明选题负责人账号与教师账号尚未完成绑定，请联系管理员并按以下步骤检查。
                </Paragraph>
                <Paragraph>
                  <ol>
                    <li>
                      使用选题负责人账号登录系统，完成初始密码修改并绑定邮箱。
                    </li>
                    <li>
                      使用管理员提供的教师账号登录系统，<Text
                      style={{color: 'red'}}>确保两个账号姓名、学院一致，并绑定相同邮箱；密码无需保持一致。</Text>
                    </li>
                    <li>
                      绑定完成后，可通过右上角菜单切换身份；若入口暂未出现，请刷新页面，仍无法使用时联系管理员。
                    </li>
                  </ol>
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    defaultActiveKey={['1']}
                    items={[{
                      key: '1',
                      label: '选题负责人端简易演示过程',
                      children: (
                        <Carousel
                          autoplay
                          dots={{className: 'custom-dots'}}
                          arrows
                          style={{margin: '0 auto'}}
                        >
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              1. 点击菜单栏的“审核”，即可查看本选题组教师提交的所有题目
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              2. 审核题目时，如需打回，需填写“打回理由”
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              3. 点击菜单栏“选题 → 选题情况”，可查看本选题组覆盖专业学生的选题情况，并支持导出详细的表格
                            </div>
                          </div>
                          <div>
                            <div style={{
                              textAlign: 'center',
                              marginTop: 12,
                              padding: '8px 12px',
                              background: '#f9f9f9',
                              borderRadius: 6,
                              color: '#555',
                              fontSize: 14,
                            }}>
                              4. 可通过右上角账号菜单退出登录或切换身份，选题负责人切换为教师后即可进行出题
                            </div>
                          </div>
                        </Carousel>
                      )
                    }]}
                  />
                </Paragraph>
              </Tabs.TabPane>
            </Tabs>
            <Title level={2}>常见问题</Title>
            <Paragraph>
              <ul>
                <li>
                  无法访问？部分地区的网络线路可能存在临时异常，可尝试刷新页面、切换网络或稍后重试。
                </li>
                <li>
                  没有账号？请先联系所在院系教师确认，再由系统管理员创建账号。
                </li>
                <li>
                  账号受限？系统检测到异常请求时可能临时限制访问，请停止频繁操作并等待自动解除；长时间未恢复时请联系管理员。
                </li>
                <li>
                  操作不清楚？请先查看对应身份的使用手册和学校通知，仍有疑问时联系指导教师、选题负责人或管理员。
                </li>
              </ul>
              如问题仍未解决，请记录出现问题的页面、操作步骤和提示信息，便于相关人员快速定位处理。
            </Paragraph>
          </Typography>
        </Card>
      </div>
      <Toc/>
    </PageContainer>
  );
};

export default Welcome;
