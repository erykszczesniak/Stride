import React from "react";
import Sidebar from "../Sidebar/Sidebar";
import { LayoutContainer, MainContent, ContentWrapper } from "./LayoutStyles";

const Layout = ({ children }) => {
  return (
    <LayoutContainer>
      <Sidebar />
      <MainContent>
        <ContentWrapper>{children}</ContentWrapper>
      </MainContent>
    </LayoutContainer>
  );
};

export default Layout;
