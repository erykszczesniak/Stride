import React from "react";
import Layout from "../components/Layout/Layout";
import styled from "styled-components";

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
`;

const ComingSoon = styled.div`
  background: white;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const Icon = styled.div`
  font-size: 64px;
  margin-bottom: 16px;
`;

const Message = styled.p`
  font-size: 16px;
  color: #666;
`;

const Clubs = () => {
  return (
    <Layout>
      <PageTitle>Clubs</PageTitle>
      <ComingSoon>
        <Icon>👥</Icon>
        <Message>Clubs page coming soon...</Message>
      </ComingSoon>
    </Layout>
  );
};

export default Clubs;
