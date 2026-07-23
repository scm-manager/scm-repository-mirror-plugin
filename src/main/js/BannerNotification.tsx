/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import { FC, ReactNode } from "react";
import styled from "styled-components";
import { Notification } from "@scm-manager/ui-core";

type NotificationType = "primary" | "info" | "success" | "warning" | "danger" | "inherit";

type Props = {
  type?: NotificationType;
  className?: string;
  children?: ReactNode;
};

const StyledBannerNotification = styled(Notification)`
  .panel &&&, .column > & {
    padding: 1.5rem;
  }
  
  .panel &&& {
    margin: 1.5rem 1.25rem 0 1.25rem;
    padding: 1.5rem;
  }

  .panel-block &&& {
    margin: 1rem 0.5rem ;
  }
  
  .progress {
    border: 1px solid #dbdbdb;
  }

  .progress::-webkit-progress-bar {
    background-color: #ffffff;
  }

  .progress.is-loading::-webkit-progress-value {
    background-image: linear-gradient(
      90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.6) 50%,
      transparent 100%
    );
    background-size: 200% 100%;
    animation: progress-shine 2.5s ease-in-out infinite;
  }

  .progress.is-loading::-moz-progress-bar {
    background-image: linear-gradient(
      90deg,
      transparent 0%,
      rgba(40, 241, 255, 1) 50%,
      transparent 100%
    );
    background-size: 200% 100%;
    animation: progress-shine 2.5s ease-in-out infinite;
  }

  @keyframes progress-shine {
    0% { background-position: 150% 0; }
    100% { background-position: -50% 0; }
  }
  
  @media (prefers-reduced-motion: reduce) {
    .progress.is-loading::-webkit-progress-value,
    .progress.is-loading::-moz-progress-bar {
      animation: none;
    }
  }
`;

const BannerNotification = StyledBannerNotification as unknown as FC<Props>;

export default BannerNotification;
