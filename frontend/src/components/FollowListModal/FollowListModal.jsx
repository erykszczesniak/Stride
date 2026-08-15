import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { FiX } from "react-icons/fi";
import UserService from "../../api/UserService";
import { getInitials } from "../../utils/formatters";
import {
  Avatar,
  AvatarInitials,
  City,
  CloseButton,
  Header,
  List,
  Modal,
  Name,
  NameColumn,
  Overlay,
  Row,
  StateMessage,
  Title,
} from "./FollowListModalStyles";

/**
 * Followers / following list for a profile. Rows navigate to the athlete's
 * profile — the same drill-down the iOS stats row offers.
 *
 * @param {boolean} isOpen
 * @param {() => void} onClose
 * @param {number} userId - whose lists to show
 * @param {"followers"|"following"} kind
 */
const FollowListModal = ({ isOpen, onClose, userId, kind }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!isOpen || !userId) return;
    let cancelled = false;
    setLoading(true);
    setError(false);
    const request =
      kind === "followers"
        ? UserService.getFollowers(userId, 0, 100)
        : UserService.getFollowing(userId, 0, 100);
    request
      .then((res) => {
        if (!cancelled) setUsers(res.data || []);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isOpen, userId, kind]);

  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const openProfile = (id) => {
    onClose();
    navigate(`/profile/${id}`);
  };

  // Portaled to <body>: any transformed ancestor (the tab panes animate with
  // translateY) becomes the containing block for position: fixed and drags
  // the overlay off-centre.
  return createPortal(
    <Overlay onClick={handleOverlayClick} data-testid="follow-list-modal">
      <Modal>
        <Header>
          <Title>
            {kind === "followers"
              ? t("profile.followList.followersTitle")
              : t("profile.followList.followingTitle")}
          </Title>
          <CloseButton onClick={onClose} aria-label={t("profile.followList.close")}>
            <FiX size={20} />
          </CloseButton>
        </Header>
        {loading ? (
          <StateMessage>{t("profile.followList.loading")}</StateMessage>
        ) : error ? (
          <StateMessage>{t("profile.followList.error")}</StateMessage>
        ) : users.length === 0 ? (
          <StateMessage>{t("profile.followList.empty")}</StateMessage>
        ) : (
          <List>
            {users.map((user, index) => (
              <Row
                key={user.id}
                onClick={() => openProfile(user.id)}
                // Short cascade, capped so long lists don't feel sluggish.
                style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
              >
                {user.avatar ? (
                  <Avatar src={user.avatar} alt="" />
                ) : (
                  <AvatarInitials>
                    {getInitials(user.firstName, user.lastName)}
                  </AvatarInitials>
                )}
                <NameColumn>
                  <Name>
                    {user.firstName} {user.lastName}
                  </Name>
                  {user.city && <City>{user.city}</City>}
                </NameColumn>
              </Row>
            ))}
          </List>
        )}
      </Modal>
    </Overlay>,
    document.body
  );
};

export default FollowListModal;
