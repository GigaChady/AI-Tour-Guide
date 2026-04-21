import uuid
from datetime import datetime, timezone
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy import Column, Float, String, Text, Boolean, DateTime, ForeignKey, Index
from geoalchemy2 import Geometry
from sqlalchemy.orm import relationship
from sqlalchemy import JSON
from sqlalchemy.orm import DeclarativeBase

class Base(DeclarativeBase):
    pass

def utc_now_naive() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String, unique=True, nullable=False)
    hashed_password = Column(String, nullable=True)
    google_id = Column(String, unique=True, nullable=True)
    name = Column(String, nullable=True)
    gender = Column(String, nullable=True)
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    narration_settings = relationship("UserNarrationSettings", back_populates="user", uselist=False)
    routes = relationship("Route", back_populates="user")
    refresh_tokens = relationship("RefreshToken", back_populates="user")

class UserNarrationSettings(Base):
    __tablename__ = "user_narration_settings"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), unique=True, nullable=False)
    settings = Column(JSON, default=dict, nullable=True)

    user = relationship("User", back_populates="narration_settings")

class UserPreferences(Base):
    __tablename__ = "user_preferences"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), unique=True)
    interests = Column(JSON, default=list)

    user = relationship("User", back_populates="preferences")

class Route(Base):
    __tablename__ = "routes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    started_at = Column(DateTime, default=utc_now_naive)
    ended_at = Column(DateTime, nullable=True)
    city = Column(String, nullable=True)
    name = Column(String, nullable=True)
    path = Column(Geometry("LINESTRING"), nullable=True)
    distance_m = Column(Float, nullable=True)

    user = relationship("User", back_populates="routes")
    pois = relationship("RoutePoi", back_populates="route")

    __table_args__ = (
        Index("ix_routes_user_id", "user_id"),
    )


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    token_hash = Column(String, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    revoked = Column(Boolean, default=False)

    user = relationship("User", back_populates="refresh_tokens")

    __table_args__ = (
        Index("ix_refresh_tokens_user_id_token_hash", "user_id"),
    )


class RoutePoi(Base):
    __tablename__ = "route_pois"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    route_id = Column(UUID(as_uuid=True), ForeignKey("routes.id"), nullable=False)
    poi_id = Column(String, nullable=True)
    name = Column(String, nullable=False)
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    description = Column(String, nullable=True)
    image_url = Column(String, nullable=True)
    received_at = Column(DateTime, default=utc_now_naive)

    route = relationship("Route", back_populates="pois")

    __table_args__ = (
        Index("ix_route_pois_route_id", "route_id"),
    )
