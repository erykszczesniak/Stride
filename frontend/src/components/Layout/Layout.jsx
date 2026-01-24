import React from "react";
import Navbar from "../Navbar/Navbar";
import { LayoutContainer, MainContent } from "./LayoutStyles";

const Layout = ({ children }) => {
  return (
    <LayoutContainer>
      <Navbar />
      <MainContent>{children}</MainContent>
    </LayoutContainer>
  );
};

export default Layout;
