import uuid
from datetime import datetime, timezone
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy import Column, Float, String, Text, Boolean, DateTime, ForeignKey, Index, Integer
from geoalchemy2 import Geometry 
from sqlalchemy.orm import relationship
from sqlalchemy import JSON
from sqlalchemy.orm import DeclarativeBase

class Base(DeclarativeBase):
    pass
#TODO: fix models after some changes

def utc_now_naive() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String, unique=True, nullable=False)
    hashed_password = Column(String, nullable=True) 
    google_id = Column(String, unique=True, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=utc_now_naive)
    name = Column(String, nullable=True) 
    imie = Column(String, nullable=True)
    nazwisko = Column(String, nullable=True)
    gender_option_id = Column(Integer, ForeignKey("demographics_gender_options.id"), nullable=True)
    gender_custom = Column(String, nullable=True)
    wiek = Column(Float, nullable=True)
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    gender_option = relationship("DemographicsGenderOption")
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
    name = Column(String, nullable=True) # moze jakis tytul trasy czy cos, zeby latwiej bylo potem rozpoznac trasy usera, ale na razie niech bedzie nullable, bo moze niektorym userom bedzie sie chcialo to wypelniac a innym nie
    # moze jeszcze jakies pola typu aktualna lokalizacja czy cos
    path = Column(Geometry("LINESTRING"), nullable=True) 
    distance_m = Column(Float, nullable=True)

    user = relationship("User", back_populates="routes")

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


class RoutePoi(Base): #TODO do ustalenia jak bede wiedzial co dostaje do workera
    __tablename__ = "route_pois"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    route_id = Column(UUID(as_uuid=True), ForeignKey("routes.id"), nullable=False)
    poi_id = Column(String, nullable=True)
    name = Column(String, nullable=False)
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    description = Column(String, nullable=True)
    image_url = Column(String, nullable=True)
    image_base64 = Column(Text, nullable=True)
    received_at = Column(DateTime, default=utc_now_naive)

    __table_args__ = (
        Index("ix_route_pois_route_id", "route_id"),
    )


class DemographicsGenderOption(Base):
    __tablename__ = "demographics_gender_options"

    id = Column(Integer, primary_key=True, autoincrement=True)
    code = Column(String, unique=True, nullable=False)
    label = Column(String, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)
    sort_order = Column(Integer, default=0, nullable=False)

