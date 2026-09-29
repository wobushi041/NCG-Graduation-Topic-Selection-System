import '@umijs/max';
export type SiderTheme = 'light' | 'dark';
export const SelectLang = () => {
  return (
    //@ts-ignore
    // eslint-disable-next-line react/jsx-no-undef
    <UmiSelectLang
      style={{
        padding: 4,
      }}
    />
  );
};
