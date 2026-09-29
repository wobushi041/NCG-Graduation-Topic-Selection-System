import { PageContainer } from '@ant-design/pro-components';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import { Button, Card, Carousel, Collapse, Divider, Tabs, theme, Typography } from 'antd';
import type { CarouselRef } from 'antd/es/carousel';
import React from 'react';
import { Toc } from '@/pages/Toc';
// eslint-disable-next-line @typescript-eslint/no-unused-vars

const { Title, Paragraph, Text } = Typography;

const GuideSlide: React.FC<{
  image: string;
  alt: string;
  description: string;
}> = ({ image, alt, description }) => {
  const { token } = theme.useToken();

  return (
    <div>
      <div style={{ maxWidth: 1120, margin: '0 auto' }}>
        <img
          src={image}
          alt={alt}
          loading="lazy"
          draggable={false}
          style={{
            display: 'block',
            width: '100%',
            aspectRatio: '16 / 10',
            objectFit: 'cover',
            objectPosition: 'top center',
            border: `1px solid ${token.colorBorderSecondary}`,
            borderRadius: 8,
            background: token.colorBgLayout,
          }}
        />
        <div
          style={{
            textAlign: 'center',
            marginTop: 12,
            padding: '10px 14px',
            background: token.colorFillAlter,
            borderRadius: 6,
            color: token.colorTextSecondary,
            fontSize: 14,
            lineHeight: 1.7,
          }}
        >
          {description}
        </div>
      </div>
    </div>
  );
};

const GuideCarousel: React.FC<React.PropsWithChildren> = ({ children }) => {
  const carouselRef = React.useRef<CarouselRef>(null);
  const { token } = theme.useToken();
  const arrowStyle: React.CSSProperties = {
    position: 'absolute',
    top: '45%',
    zIndex: 2,
    width: 38,
    height: 38,
    color: token.colorPrimary,
    background: token.colorBgElevated,
    border: `1px solid ${token.colorBorder}`,
    boxShadow: token.boxShadowSecondary,
    transform: 'translateY(-50%)',
  };

  return (
    <div style={{ position: 'relative', padding: '0 44px' }}>
      <Carousel ref={carouselRef} autoplay autoplaySpeed={5000} dots={{ className: 'custom-dots' }}>
        {children}
      </Carousel>
      <Button
        shape="circle"
        aria-label="查看上一步演示"
        icon={<LeftOutlined />}
        onClick={() => carouselRef.current?.prev()}
        style={{ ...arrowStyle, left: 0 }}
      />
      <Button
        shape="circle"
        aria-label="查看下一步演示"
        icon={<RightOutlined />}
        onClick={() => carouselRef.current?.next()}
        style={{ ...arrowStyle, right: 0 }}
      />
    </div>
  );
};

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
}> = ({ title, href, index, desc }) => {
  const { useToken } = theme;

  const { token } = useToken();

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
  const { token } = theme.useToken();
  const { initialState } = useModel('@@initialState');
  const watermarkFreeGuideStyle: React.CSSProperties = {
    position: 'relative',
    zIndex: 1000,
    background: token.colorBgContainer,
    borderRadius: token.borderRadiusLG,
  };

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
          <Title style={{ textAlign: 'center' }}>欢迎使用广州南方学院毕设选题管理系统🎉</Title>
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
          <Divider />
          <Typography>
            <Title level={2}>使用指南</Title>
            <Tabs defaultActiveKey="1">
              <Tabs.TabPane tab="学生使用手册" key="1">
                <Paragraph>学生操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>持续关注并浏览教师发布的毕业设计题目</li>
                      <li>确认预选毕业设计题目或取消预选毕业设计题目</li>
                      <li>确认提交毕业设计题目或取消提交毕业设计题目</li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  学生可先浏览教师发布的<Text strong>“毕业设计题目”（以下简称“题目”）</Text>
                  ，并根据个人意向进行预选。
                  <Text underline>题目开放后</Text>，每名学生最多可预选 10
                  个题目；进入学生选题阶段后，需从可选题目中确认并提交一个最终题目。
                  提交成功后如需变更，请先按系统规则取消已提交题目，或联系指导教师协助处理。
                </Paragraph>
                <Paragraph>
                  本系统 <Text mark>支持在允许的阶段退选，请谨慎操作</Text>
                  。若退选入口不可用或无法取消已提交题目，请联系对应指导教师处理。
                </Paragraph>
                <Paragraph>
                  选题过程中请勿使用连点器、自动刷新脚本等工具。
                  <Text style={{ color: 'red' }}>系统检测到异常请求后可能临时限制账号访问</Text>
                  ，并影响正常选题安排。
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    style={watermarkFreeGuideStyle}
                    defaultActiveKey={['1']}
                    items={[
                      {
                        key: '1',
                        label: '学生端简易演示过程',
                        children: (
                          <>
                            <GuideCarousel>
                              <GuideSlide
                                image="/steps/screenshots/student-1-preselect.png"
                                alt="学生端预先选题教师列表"
                                description="1. 点击“学生选题 → 预先选题”，浏览当前可选教师及其题目数量。"
                              />
                              <GuideSlide
                                image="/steps/screenshots/student-2-topic.png"
                                alt="学生端查看教师题目并预选"
                                description="2. 点击“查看题目”，核对题目内容、剩余名额和时间后进行预选。"
                              />
                              <GuideSlide
                                image="/steps/screenshots/student-3-submit.png"
                                alt="学生端提交最终选题"
                                description="3. 进入“提交选题”，从已预选题目中确认一个最终题目，也可取消预选。"
                              />
                              <GuideSlide
                                image="/steps/screenshots/student-4-view.png"
                                alt="学生端查看最终选题详情"
                                description="4. 进入“查看选题”，查看最终题目的要求、指导教师、选题组及选题时间。"
                              />
                            </GuideCarousel>
                          </>
                        ),
                      },
                    ]}
                  />
                </Paragraph>
              </Tabs.TabPane>
              <Tabs.TabPane tab="教师使用手册" key="2">
                <Paragraph>教师操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>发布题目或修改题目</li>
                      <li>查看题目状态</li>
                      <li>根据实际情况为审核通过的题目选择学生（双选）</li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  教师应在规定时间内提交<Text strong>“毕业设计题目”（以下简称“题目”）</Text>
                  ，并及时关注审核状态。 不同教师的出题数量上限以系统配置为准；
                  <Text underline>题目开放后</Text>，可根据选题规则查看并选择符合条件的学生。
                </Paragraph>
                <Paragraph>
                  本系统 <Text mark>支持在允许的阶段协助学生退选，请谨慎操作</Text>
                  。若退选功能不可用，请联系选题负责人或系统管理员处理。
                </Paragraph>
                <Paragraph>
                  发布题目前可使用<Text strong> AI 校验工具</Text>
                  辅助检查题目与近三年题目的相似情况，结果仅供参考。
                  <Text style={{ color: 'red' }}>
                    每位教师每日最多使用 30 次，请合理安排使用次数。
                  </Text>
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    style={watermarkFreeGuideStyle}
                    defaultActiveKey={['1']}
                    items={[
                      {
                        key: '1',
                        label: '教师端简易演示过程',
                        children: (
                          <GuideCarousel>
                            <GuideSlide
                              image="/steps/screenshots/teacher-1-list.png"
                              alt="教师端已发布题目列表"
                              description="1. 点击“教师发布 → 发布题目和修改题目”，集中查看本人题目、状态与剩余名额。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-2-add.png"
                              alt="教师端添加题目表单"
                              description="2. 点击“添加题目”，填写标题、类型、描述、学生要求、容量和适用选题组。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-2-add.png"
                              alt="教师端提交题目与相似度检测"
                              description="3. 可先使用 AI 相似度检测作为参考，确认无误后提交并等待选题负责人审核。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-4-edit.png"
                              alt="教师端编辑被打回题目"
                              description="4. 对未发布题目点击“编辑”，直接在表格中修改内容并保存。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-3-rejected.png"
                              alt="教师端查看题目打回原因"
                              description="5. 题目被打回时先阅读打回理由，修改完成后点击“重新提交审核”。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-5-select-student.png"
                              alt="教师端为已发布题目选择学生"
                              description="6. 题目发布后进入“选择学生”，按学号、姓名、学院或专业筛选并完成双选。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/teacher-6-selected-student.png"
                              alt="教师端查看已选学生并协助退选"
                              description="7. 在“查看选择自己的学生”中查看最终名单，必要时可按规则协助学生退选。"
                            />
                          </GuideCarousel>
                        ),
                      },
                    ]}
                  />
                </Paragraph>
              </Tabs.TabPane>
              <Tabs.TabPane tab="选题负责人使用手册" key="3">
                <Paragraph>选题负责人操作流程：</Paragraph>
                <Paragraph>
                  <Text strong>
                    <ul>
                      <li>审核题目（通过题目、打回题目）</li>
                      <li>查看本学院学生的选题情况</li>
                      <li>快速导出选题情况表格文件</li>
                    </ul>
                  </Text>
                </Paragraph>
                <Paragraph>
                  选题负责人应在 <Text underline>双选开始前</Text> 完成本选题组所有
                  <Text strong>“毕业设计题目”（以下简称“题目”）</Text>
                  的审核。被打回的题目可由教师根据意见修改后重新提交。
                </Paragraph>
                <Paragraph>
                  本系统支持选题负责人在满足条件时切换为教师身份进行出题。若右上角菜单中
                  <Text underline>未显示“切换身份”入口</Text>
                  ，说明选题负责人账号与教师账号尚未完成绑定，请联系管理员并按以下步骤检查。
                </Paragraph>
                <Paragraph>
                  <ol>
                    <li>使用选题负责人账号登录系统，完成初始密码修改并绑定邮箱。</li>
                    <li>
                      使用管理员提供的教师账号登录系统，
                      <Text style={{ color: 'red' }}>
                        确保两个账号姓名、学院一致，并绑定相同邮箱；密码无需保持一致。
                      </Text>
                    </li>
                    <li>
                      绑定完成后，可通过右上角菜单切换身份；若入口暂未出现，请刷新页面，仍无法使用时联系管理员。
                    </li>
                  </ol>
                </Paragraph>
                <Paragraph>
                  <Collapse
                    size="small"
                    style={watermarkFreeGuideStyle}
                    defaultActiveKey={['1']}
                    items={[
                      {
                        key: '1',
                        label: '选题负责人端简易演示过程',
                        children: (
                          <GuideCarousel>
                            <GuideSlide
                              image="/steps/screenshots/leader-1-review.png"
                              alt="选题负责人审核待审题目"
                              description="1. 点击“审核”，查看本选题组教师提交的待审核题目并选择通过或打回。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/leader-2-reject.png"
                              alt="选题负责人填写题目打回原因"
                              description="2. 需要打回时填写清晰、可执行的原因，便于教师按要求修改后重新提交。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/leader-3-situation.png"
                              alt="选题负责人查看并导出选题情况"
                              description="3. 点击“选题 → 选题情况”，查看本选题组统计，并按需导出已选或未选名单。"
                            />
                            <GuideSlide
                              image="/steps/screenshots/leader-4-account-menu.png"
                              alt="选题负责人账号菜单与身份切换"
                              description="4. 从右上角账号菜单切换教师身份进行出题，或在操作完成后安全退出登录。"
                            />
                          </GuideCarousel>
                        ),
                      },
                    ]}
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
                <li>没有账号？请先联系所在院系教师确认，再由系统管理员创建账号。</li>
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
      <Toc />
    </PageContainer>
  );
};

export default Welcome;
